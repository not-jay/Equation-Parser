package com.xtouchme.come326.base;

public class Equation {

	private String equation = "";
	private String backup = "";
	
	private String[] trigFunctions = {"sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sqrt", "cbrt"};
	//I know sqrt/cbrt isn't a trigonometric function, but with how the program flows, it's best to break it down here
	//with the rest of the trig functions. Same goes for the hyperbolic functions
	
	private static final int SIN = 0;
	private static final int COS = 1;
	private static final int TAN = 2;
	private static final int ASIN = 3;
	private static final int ACOS = 4;
	private static final int ATAN = 5;
	private static final int SINH = 6;
	private static final int COSH = 7;
	private static final int TANH = 8;
	private static final int SQRT = 9;
	private static final int CBRT = 10;
	
	public static Equation createEquation(String eqn) {
		return new Equation().setEqn(eqn);
	}
	
	private Equation setEqn(String eqn) {
		this.equation = this.backup = eqn;
		return this;
	}
	
	public boolean containsVariables() {
		for(String functions : trigFunctions) {
			equation = equation.replace(functions, "TRIGO!");
		}
		boolean vars = equation.replaceAll("[a-z]", "VARIABLE!").contains("VARIABLE!");
		equation = backup;
		
		return vars;
	}
	
	//The parsing is split into three parts
	//This parses the whole of the equation, splitting it into sub-equations through parenthesis
	//It takes the inner most sub-equation and parses it
	public float parseEqn(float startingValue) {
		float ans = 0;
		
		equation = backup;
		equation = parseTrigFunctions(startingValue);
		
		int open = countChars(equation, '(');
		int close = countChars(equation, ')');
		
		//Just incase the user mistakenly inputs the parenthesis, It removes the last unpaired ones until every thing
		//is paired again
		if(open < close) {
			int delta = close - open;
			for(int x = 0; x < delta; x++) {
				equation = equation.substring(0, equation.lastIndexOf(')')) + equation.substring(equation.lastIndexOf(')')+1);
				close--;
			}
		} else if (open > close) {
			int delta = open - close;
			for(int x = 0; x < delta; x++) {
				equation = equation.substring(0, equation.indexOf('(')) + equation.substring(equation.indexOf('(')+1);
				open--;
			}
		}
		
		//No parenthesis found, the whole equation is a sub-equation
		if(open <= 0)
			ans = parseLine(equation, startingValue);
		else {
			//Split into sub-equations, parse, and plug back in the original equation
			for(int x = 0; x < open; x++) {
				int first = equation.lastIndexOf("(")+1;
				int second = equation.indexOf(")", first);
				String temp = equation.substring(first, second);
				ans = parseLine(temp, startingValue);
				equation = equation.replace("("+temp+")", String.valueOf(ans));
			}
			//At this point, the equation should be reduced to either a numberical value or a simple 2 term equation.
			ans = parseLine(equation, startingValue);
		}
		
		return ans;
	}
	
	//Parses sub-equations
	private float parseLine(String operation, float startingValue) {
		int operators = countChars(operation, '^', '*', '/', '+', '-');
		float ans = 0;
		String[] operands = null;
		String delimiters = "^*/+-";
		
		if(operators <= 0) ans = Float.parseFloat(operation);
		else {
			for(int x = 0; x < operators; x++) {
				for(char ch : delimiters.toCharArray()) {
					if(!operation.contains(String.valueOf(ch))) continue;

					if(operation.contains(" ")) operands = operation.split(" ");
					else operands = padOperators(operation).split(" ");
					
					for(int y = 0; y < operands.length; y++) {
						if(operands[y].length() > 1 && countChars(operands[y], delimiters.toCharArray()) >= 1) {
							ans = parseLine(operands[y], startingValue);
							operands[y] = String.valueOf(ans);
						}
						else if(operands[y].equals(String.valueOf(ch))) {
							if(operands[y-1].isEmpty()) operands[y-1] = "0";
							String temp = operands[y-1] + operands[y] + operands[y+1];
							ans = parse(temp, startingValue);
							operands[y] = String.valueOf(ans);
							operands[y-1] = operands[y+1] = "";
							operands = removeInvalid(operands);
						}
					}

					operation = concatenateArray(operands);
				}
			}
		}
		
		return ans;
	}
	
	//Parses a simple 2 term equation (ie. 2+2) or just a value
	private float parse(String operation, float startingValue) {
		String[] operands = null;
		float ans = 0;
		String delimiters = "^*/+-";
		
		//If it's just a value...
		if(countChars(operation, delimiters.toCharArray()) == 0)
			return Float.parseFloat(operation);
		
		//If it isn't a value, it is a simple operation, with 2 operands and 1 operator
		for(char ch : delimiters.toCharArray()) {
			//Find what the operator is
			if(operation.contains(String.valueOf(ch))) {
				//Split the string with the operator as the delimiter
				operands = operation.split("\\"+String.valueOf(ch));
				//Replace the variable with the startingValue
				operands[0] = operands[0].replaceAll("[a-z]", String.valueOf(startingValue));
				operands[1] = operands[1].replaceAll("[a-z]", String.valueOf(startingValue));
				
				try {
					switch(ch) {
					case '^':
						ans = (float) Math.pow(Double.parseDouble(operands[0]), Double.parseDouble(operands[1]));
						break;
					case '*':
						ans = Float.parseFloat(operands[0]) * Float.parseFloat(operands[1]);
						break;
					case '/':
						ans = Float.parseFloat(operands[0]) / Float.parseFloat(operands[1]);
						break;
					case '+':
						ans = Float.parseFloat(operands[0]) + Float.parseFloat(operands[1]);
						break;
					case '-':
						ans = Float.parseFloat(operands[0]) - Float.parseFloat(operands[1]);
						break;
					}
				} catch (NumberFormatException e) {
					if(e.getMessage().contains("I")) {
						System.out.println(" = Infinity");
						System.out.println("Closing program to prevent other errors");
						System.exit(1); //Close the program to prevent any other errors
					}
				}

				break;
			}
		}
		
		return ans;
	}
	
	//Processes trigonometric functions and sq and cb roots before everything else.
	private String parseTrigFunctions(float value) {
		String newEquation = backup;
		
		for(String function : trigFunctions) {
			if(!newEquation.contains(function)) continue;
			
			while(newEquation.contains(function)) {
				int first = newEquation.lastIndexOf(function+"(")+1+function.length();
				int second = newEquation.indexOf(")", first);
				String temp = newEquation.substring(first, second);
				Equation tempEqn = Equation.createEquation(temp);
				tempEqn = Equation.createEquation(function+"("+String.valueOf(tempEqn.parseEqn(value))+")");
				newEquation = newEquation.replace(function+"("+temp+")", String.valueOf(tempEqn.parseTrig()));
			}
		}
		
		return newEquation;
	}
	
	private float parseTrig() {
		float ans = 0;
		int x = 0;
		
		for(String function : trigFunctions) {
			if(!equation.contains(function)) {
				x++;
				continue;
			}
			
			String temp = equation.substring(equation.indexOf('(')+1, equation.indexOf(')'));
			float value = Float.parseFloat(temp);
			
			switch(x) {
				case SIN:
					ans = (float)Math.sin(Math.toRadians(value));
					break;
				case COS:
					ans = (float)Math.cos(Math.toRadians(value));
					break;
				case TAN:
					ans = (float)Math.tan(Math.toRadians(value));
					break;
				case ASIN:
					ans = (float)Math.asin(Math.toRadians(value));
					break;
				case ACOS:
					ans = (float)Math.acos(Math.toRadians(value));
					break;
				case ATAN:
					ans = (float)Math.atan(Math.toRadians(value));
					break;
				case SINH:
					ans = (float)Math.sinh(Math.toRadians(value));
					break;
				case COSH:
					ans = (float)Math.cosh(Math.toRadians(value));
					break;
				case TANH:
					ans = (float)Math.tanh(Math.toRadians(value));
					break;
				case SQRT:
					ans = (float)Math.sqrt(value);
					break;
				case CBRT:
					ans = (float)Math.cbrt(value);
					break;
			}
			
			x++;
		}
		
		return ans;
	}
	
	private String padOperators(String array) {
		String newString = "";
		
		for(char c : array.toCharArray()) {
			if(!charEquals(c, '^', '*', '/', '+', '-')) newString += c;
			else newString += " "+c+" ";
		}
		
		return newString;
	}
	
	private boolean charEquals(char c, char... ds) {
		for(char ch : ds) {
			if(ch == c) return true;
		}
		return false;
	}
	
	private String[] removeInvalid(String[] array) {
		int count = 0;
		for(String s : array) {
			if(!s.isEmpty()) count++;
		}
		
		String[] newArray = new String[count];
		for(int x = 0, y = 0; x < array.length; x++) {
			if(!array[x].isEmpty()) newArray[y++] = array[x];
		}
		
		return newArray;
	}
	
	private String concatenateArray(String[] array) {
		String str = "";
		
		for(String s : array) {
			str += " " + s;
		}
		
		return str.substring(1);
	}
	
	@SuppressWarnings("unused")
	private int getCharPosition(String str, char... c) {
		int pos = 0;
		
		for(char ch : c) {
			if(str.contains(String.valueOf(ch))) {
				pos = str.indexOf(ch);
				break;
			}
		}
		
		return pos;
	}
	
	@SuppressWarnings("unused")
	private int countChars(String str, String chars) {
		return countChars(str, chars.toCharArray());
	}
	
	private int countChars(String str, char... c) {
		int count = 0;
		
		for(char ch : c) {
			count += countChars(str, ch);
		}
		
		return count;
	}
	
	private int countChars(String str, char c) {
		int count = 0;
		for(int x = 0; x < str.length(); x++) {
			if(str.charAt(x) == c) count++;
		}
		
		return count;
	}
	
	@Override
	public String toString() {
		return equation;
	}
	
}

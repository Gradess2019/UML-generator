package com.gradesscompany;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for UML code generating
 */
class CodeGenerator {

	/**
	 * Contains lines of code with fields
	 */
	private String fields;

	/**
	 * Contains lines of code with methods
	 */
	private String methods;

	/**
	 * Contains lines of code with getters and setters
	 */
	private String gettersAndSetters;

	/**
	 * Translate source code to UML code
	 * @param code text from codeTextPane
	 * @param umlTextPane target textPane for UML text
	 */
	void translateFromSourceCode(String code, JTextPane umlTextPane) {

		umlTextPane.setText("");

		//Delete all cyrillic
		code = code.replaceAll("[а-яА-Я]+\\s*", "");
		//Delete all comments
		code = code.replaceAll("//.*|/\\**[\\s\\W\\w]*\\*/", "");

		if (code.contains("class")) {
			addClassName(code, umlTextPane);
		}

		findFieldsAndMethods(code);
		addFields(umlTextPane);
		addMethods(umlTextPane);
	}

	/**
	 * Searching fields and methods by checking every string
	 * @param code text from codeTextPane
	 */
	private void findFieldsAndMethods(String code) {
		ArrayList<String> codeStrings = new ArrayList<>(Arrays.asList(removeByRegEx(".*\\[SerializeField].*|.*using.*", code).split("\n")));

		StringBuilder fieldsBuilder = new StringBuilder();
		StringBuilder methodsBuilder = new StringBuilder();
		StringBuilder gettersSettersBuilder = new StringBuilder();

		for (int index = 0; index < codeStrings.size(); index++) {
			codeStrings.set(index, removeByRegEx("^\\s+|\\s+=[^;]*|$\\s+", codeStrings.get(index)));
			String codeString = codeStrings.get(index);

			if (codeString.isEmpty()) {
				codeStrings.remove(index);
				index--;
			} else if (isSubClass(codeStrings, codeString)) {
				index = removeBodyWithHead(codeStrings, codeString, index);
			} else if (isAnEnum(codeString)) {
				removeBody(codeStrings, codeString, index);
				fieldsBuilder.append(codeString).append("\n");
			} else if (isGetterOrSetter(codeString)) {
				gettersSettersBuilder.append(codeString).append("\n");
			} else if (isMethod(codeString)) {
				removeBody(codeStrings, codeString, index);
				codeString = removeByRegEx("(?<=\\w)\\s+\\w+(?=[),])|\\s*;\\s*$|\\s+$", codeString);
				methodsBuilder.append(codeString).append("\n");
			} else if (isField(codeString)) {
				fieldsBuilder.append(codeString).append("\n");
			}
		}

		fields = fieldsBuilder.toString();
		methods = methodsBuilder.toString();
		gettersAndSetters = gettersSettersBuilder.toString();
	}

	/**
	 * @param code text from codeTextPane
	 * @param umlTextPane target textPane for UML text
	 */
	private void addClassName(String code, JTextPane umlTextPane) {
		String classString = findClassString(code);

		SimpleAttributeSet attributeSet = new SimpleAttributeSet();

		if (isAbstract(classString)) {
			StyleConstants.setItalic(attributeSet, true);
		}

		String className = removeByRegEx(".*\\s*class\\s*|\\s*:.*\\s*", classString) + "\n";

		addTextToTextPane(umlTextPane, attributeSet, className);
	}

	/**
	 * Adding fields to UML text pane
	 * @param umlTextPane target textPane for UML text
	 */
	private void addFields(JTextPane umlTextPane) {

		if (fields.isEmpty()) {
			return;
		}

		String[] fieldsArray = fields.split("\n");

		for (String codeString : fieldsArray) {

			SimpleAttributeSet attributeSet = new SimpleAttributeSet();
			if (isStatic(codeString)) {
				StyleConstants.setUnderline(attributeSet, true);
			}

			String[] words = removeByRegEx("\\s*;\\s*|(?<=\\s)\\s", codeString).split(" ");

			String nameVariable = words[words.length - 1];
			String typeVariable = words[words.length - 2];

			String result = getAccessSpecifier(codeString) + " " + nameVariable + ": " + typeVariable + getFieldGetterAndSetter(nameVariable) + "\n";

			addTextToTextPane(umlTextPane, attributeSet, result);
		}
	}

	/**
	 * Adding methods to UML text pane
	 * @param umlTextPane target textPane for UML text
	 */
	private void addMethods(JTextPane umlTextPane) {

		if (methods.isEmpty()) {
			return;
		}

		String[] methodsArray = methods.split("\n");

		final String methodNameRegEx = "\\w+\\(.*\\)";
		final String accessSpecifierRegEx = "(public|protected|private|default)\\s+";
		final String abstractRegEx = "(abstract)\\s+";
		final String staticRegEx = "(static)\\s+";
		final String unnecessaryWordRegEx = "(virtual|new|override)\\s+";

		for (String codeString : methodsArray) {

			String methodName = null;
			String accessSpecifier = getAccessSpecifier(codeString);
			codeString = removeByRegEx(accessSpecifierRegEx, codeString);
			codeString = removeByRegEx(unnecessaryWordRegEx, codeString);
			Matcher matcher = Pattern.compile(methodNameRegEx).matcher(codeString);
			if (matcher.find()) {
				methodName = codeString.substring(matcher.start(), matcher.end());
				codeString = removeByRegEx("\\s*" + methodNameRegEx, codeString);
			}

			SimpleAttributeSet attributeSet = new SimpleAttributeSet();
			if (isAbstract(codeString)) {
				StyleConstants.setItalic(attributeSet, true);
				codeString = removeByRegEx(abstractRegEx, codeString);
			}

			if (isStatic(codeString)) {
				StyleConstants.setUnderline(attributeSet, true);
				codeString = removeByRegEx(staticRegEx, codeString);
			}

			codeString = removeByRegEx("\\s+", codeString);
			String methodReturnType = codeString;

			String result = accessSpecifier + " " + methodName;

			if (!methodReturnType.isEmpty()) {
				result += ": " + methodReturnType;
			}

			result += "\n";

			addTextToTextPane(umlTextPane, attributeSet, result);
		}
	}

	/**
	 * Adding text to text pane
	 * @param umlTextPane target textPane for UML text
	 * @param attributeSet attributes for text (e.g. Underline, Italic style, etc)
	 * @param text text which to be added
	 */
	private void addTextToTextPane(JTextPane umlTextPane, SimpleAttributeSet attributeSet, String text) {
		try {
			Document document = umlTextPane.getStyledDocument();
			document.insertString(document.getLength(), text, attributeSet);
		} catch (BadLocationException e) {
			umlTextPane.setText(umlTextPane.getText() + text);
		}
	}

	/**
	 * Searching class declaration in whole code, if found then return
	 * @param code text from codeTextPane
	 * @return class declaration line code
	 */
	private String findClassString(String code) {
		int indexOfStartString = code.indexOf("class");
		char charAt = code.charAt(indexOfStartString);
		while (indexOfStartString > 0 && charAt != '\t' && charAt != '\n') {
			charAt = code.charAt(indexOfStartString--);
		}
		indexOfStartString += 2;
		return removeByRegEx("^[\\s*]", code.substring(indexOfStartString, code.indexOf("\n", indexOfStartString)));
	}

	/**
	 * @param codeString line of code to search
	 * @return access specifier of field or method
	 */
	private String getAccessSpecifier(String codeString) {
		String accessSpecifier;
		if (codeString.contains("public ")) {
			accessSpecifier = "+";
		} else if (codeString.contains("protected ")) {
			accessSpecifier = "#";
		} else {
			accessSpecifier = "-";
		}
		return accessSpecifier;
	}

	/**
	 * Searching getter and/or setter of field
	 * @param codeString line of code to search
	 * @return stereotype for field in UML text pane
	 */
	private String getFieldGetterAndSetter(String codeString) {
		String[] getterAndSettersArray = gettersAndSetters.split("\n");

		String result = "  ";
		for (String getterOrSetter : getterAndSettersArray) {
			if (getterOrSetter.toLowerCase().contains(codeString.toLowerCase())) {
				result += "«";
				if (getterOrSetter.contains("get;")) {
					result += "get";
				}

				if (getterOrSetter.contains("set;")) {
					if (result.contains("get")) {
						result += ", set";
					} else {
						result += "set";
					}
				}
				result += "» ";
				break;
			}
		}
		return result;
	}

	/**
	 * Deleting chars by regular expression
	 * @param regEx regular expression
	 * @param codeString line of code to edit
	 * @return edited line code
	 */
	private String removeByRegEx(String regEx, String codeString) {
		return codeString.replaceAll(regEx, "");
	}

	/**
	 * Deleting declaration and its definition (e.g. method/subclass/etc.)
	 * @param codeStrings array of lines of code
	 * @param codeString line of code to delete
	 * @param index current index of @codeString in array
	 * @return new index for correct loop
	 */
	private int removeBodyWithHead(ArrayList<String> codeStrings, String codeString, int index) {

		codeStrings.remove(codeString);
		index--;

		return removeBody(codeStrings, codeString, index);
	}

	/**
	 * Deleting only definition
	 * @param codeStrings array of lines of code
	 * @param codeString line code declaration
	 * @param index current index of @codeString in array
	 * @return new index for correct loop
	 */
	private int removeBody(ArrayList<String> codeStrings, String codeString, int index) {
		int brackets = 0;

		if (codeString.contains("{")) {
			brackets++;
		}

		for (int subIndex = index + 1; subIndex < codeStrings.size(); subIndex++) {
			String subCodeString = codeStrings.get(subIndex);

			if (subCodeString.contains("{")) {
				brackets++;
			}

			if (subCodeString.contains("}")) {
				brackets--;
			}

			if (brackets == 0) {
				if (subCodeString.contains("{") || subCodeString.contains("}")) {
					codeStrings.remove(subIndex);
				}
				break;
			}

			codeStrings.remove(subIndex);
			subIndex--;
		}
		return index;
	}

	/**
	 * Check field or method for static
	 * @param codeString line of code to check
	 * @return is it static field or method
	 */
	private boolean isStatic(String codeString) {
		return codeString.contains("static ");
	}

	/**
	 * Check is a class or method abstract
	 * @param codeString line of code to check
	 * @return is it abstract method or class
	 */
	private boolean isAbstract(String codeString) {
		return codeString.contains("abstract ");
	}

	/**
	 * Check whether the line code is a field
	 * @param codeString line of code to check
	 * @return is it field
	 */
	private boolean isField(String codeString) {
		return !isGetterOrSetter(codeString) && !codeString.contains("class") &&
				!codeString.contains("(") && !codeString.contains(")") &&
				!codeString.contains("{") && !codeString.contains("}") &&
				codeString.split(" ").length > 1;
	}

	/**
	 * Check the line code is it a getter or setter
	 * @param codeString line of code to check
	 * @return is it getter or setter
	 */
	private boolean isGetterOrSetter(String codeString) {
		return codeString.contains("get;") || codeString.contains("set;");
	}

	/**
	 * Check the line code is it a method
	 * @param codeString line of code to check
	 * @return is it a method
	 */
	private boolean isMethod(String codeString) {
		return codeString.contains("(") && codeString.contains(")");
	}

	/**
	 * Check the line code is it an enumeration
	 * @param codeString line of code to check
	 * @return is it an enumeration
	 */
	private boolean isAnEnum(String codeString) {
		return codeString.contains("enum ");
	}

	/**
	 * Check line code is it a subclass (not a main class)
	 * @param codeStrings array of lines of code
	 * @param codeString line of code to check
	 * @return is it a subclass
	 */
	private boolean isSubClass(ArrayList<String> codeStrings, String codeString) {
		if (!codeString.contains("class ")) {
			return false;
		}

		for (int index = 0; index < codeStrings.size(); index++) {
			if (index >= codeStrings.indexOf(codeString)) {
				return false;
			}

			String element = codeStrings.get(index);

			if (element.contains("class ")) {
				int brackets = 0;
				if (element.contains("{")) {
					brackets++;
				}

				for (int subIndex = index + 1; subIndex < codeStrings.size(); subIndex++) {
					String subCodeString = codeStrings.get(subIndex);
					if (subCodeString.contains("{")) {
						brackets++;
					}

					if (subCodeString.contains("}")) {
						brackets--;
					}
					if (brackets != 0 && subCodeString.equals(codeString)) {

						return true;
					}

					if (brackets == 0) {
						break;
					}
				}
			}
		}
		return false;
	}



	//Old version of program
	//	String translateFromSourceCode(String code) {
//		StringBuilder builder = new StringBuilder();
//		if (code.contains("class")) {
//			code = code.replaceFirst("\\{", "");
//		}
//		String[] codeStrings = code.replaceAll("[\\t]+|;|([\\s]=.*)|(using).*|([\\s]:.*)[{}]|\\{[\\s\\w\\W]*?\\}\\s|(?<=\\})([\\s\\w]+?)|(\\}*?\\s*(if)\\s*.*\\s*)|(\\}*?\\s*(else)\\s*.*?)|\\}", "")
//				.replaceAll("\\[SerializeField]|\\s*const|\\s*new|\\s*override|\\s*static|\\s*virtual|\\s*abstract|\\s*:.*", "")
//				.replaceAll("\\s\\w+(?=\\))", "")
//				.split("\n");
//
//		for(int id = 0; id < codeStrings.length; id++){
//			for (int charId = 0; charId < codeStrings[id].length(); charId++) {
//				codeStrings[id] = codeStrings[id].replaceFirst("^[ \\s]+", "");
//			}
//		}
//
//		System.out.println(Arrays.toString(codeStrings));
//		for (String codeString : codeStrings) {
//			String result = generateUML(codeString.split("(?<!,)[\\s]"));
//			if (result != null) {
//				builder.append(result);
//			}
//		}
//		return builder.toString();
//	}
//
//	private String generateUML(String[] sourceCodeSElements) {
//		if (sourceCodeSElements.length == 3) {
//			String accessSpecifier = sourceCodeSElements[0];
//			String type = sourceCodeSElements[1];
//			String nameVariable = sourceCodeSElements[2];
//
//			switch (accessSpecifier) {
//				case "public": {
//					accessSpecifier = "+";
//					break;
//				}
//				case "protected": {
//					accessSpecifier = "#";
//					break;
//				}
//				case "private": {
//					accessSpecifier = "-";
//					break;
//				}
//			}
//
//			return accessSpecifier + " " + nameVariable + ": " + type + "\n";
//		} else {
//			return null;
//		}
//	}
}

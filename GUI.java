package com.gradesscompany;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("FieldCanBeLocal")
public class GUI extends JFrame {

	private static int WIDTH = 500;
	private static int HEIGHT = 400;

	private JTextPane codeTextPane;
	private JTextPane umlTextPane;

	private CodeGenerator codeGenerator;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(GUI::new);
	}

	private GUI() {
		super("UML генератор");
		setUpFrame();
		createGUIElements();
		setVisible(true);

		codeGenerator = new CodeGenerator();
	}

	private void setUpFrame() {

		setSize(WIDTH, HEIGHT);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
	}

	private void createGUIElements() {
		JButton buttonGenerator = new JButton("Сгенерировать");
		buttonGenerator.addMouseListener(mouseAdapter);

		codeTextPane = new JTextPane();
		umlTextPane = new JTextPane();

		JPanel codePanel = generatePanel(codeTextPane, Color.RED);
		JPanel umlPanel = generatePanel(umlTextPane, Color.BLUE);
		JPanel centerPanel = new JPanel(new GridLayout(2, 1));
		centerPanel.add(codePanel);
		centerPanel.add(umlPanel);

		add(buttonGenerator, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	private JPanel generatePanel(JTextPane textArea, Color borderColor) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createLineBorder(borderColor));
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);
		return panel;
	}

	private MouseAdapter mouseAdapter = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			codeGenerator.translateFromSourceCode(codeTextPane.getText(), umlTextPane);
			super.mouseClicked(e);
		}
	};
}

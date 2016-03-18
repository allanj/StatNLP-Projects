package com.statnlp.ui.visualize;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import com.statnlp.ui.visualize.type.VNode;

public class VertexPropertyDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	
	VNode node;
	private JTextField textField_IDarray;
	private JLabel lblIdArray;
	private JTextArea textArea_content;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			VertexPropertyDialog dialog = new VertexPropertyDialog(null, null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public VertexPropertyDialog(java.awt.Frame parent, final VNode node) {
		
		super(parent, true);
		this.node = node;
		
		setBounds(100, 100, 227, 178);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			lblIdArray = new JLabel("ID array:");
		}
		{
			textField_IDarray = new JTextField();
			textField_IDarray.setColumns(10);
			textField_IDarray.setText(showIDarray(node));
		}
		{
			textArea_content = new JTextArea();
			textArea_content.setText(showContent(node));
		}
		
		JLabel lblContent = new JLabel("Content:");
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addComponent(lblIdArray)
							.addGap(5))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addComponent(lblContent)
							.addPreferredGap(ComponentPlacement.RELATED)))
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING, false)
						.addComponent(textArea_content, Alignment.LEADING)
						.addComponent(textField_IDarray, Alignment.LEADING))
					.addContainerGap(72, Short.MAX_VALUE))
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addGap(11)
							.addComponent(lblIdArray))
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addGap(5)
							.addComponent(textField_IDarray, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
					.addGap(18)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(textArea_content, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
						.addComponent(lblContent))
					.addContainerGap())
		);
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int[] node_array = parseIDarray(textField_IDarray.getText());
						
						//Assume only bIndex is only to change
						node.bIndex = node_array[0];
						node.update_bIndex_downwards();
						
						String content = textArea_content.getText();
						
						node.setContent(content);
						
						
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		setTitle("VNode: " + node.toString());
	}
	
	int[] parseIDarray(String id_arr_str)
	{
		String[] nodeID_arr_str = id_arr_str.split(",");
		int[] nodeID_arr = new int[5];
		
		for(int i = 0; i < nodeID_arr_str.length; i++)
		{
			String d = nodeID_arr_str[i];
			
			if (d.equals("?"))
			{
				return null;
			}
			else
			{
				nodeID_arr[i] = Integer.parseInt(d);
			}
		}
		
		return nodeID_arr;
		
	}
	
	String showIDarray(VNode node)
	{
		int[] nodeID_arr;
		
		nodeID_arr = node.getNodeIDArray();
		
		
		StringBuffer sb = new StringBuffer("");
		for(int i = 0; i < nodeID_arr.length; i++)
		{
			if (i > 0)
				sb.append(",");
			
			int d = nodeID_arr[i];
			if (d == VNode.UNDEFINED)
				sb.append("?");
			else
				sb.append(Integer.toString(d));
		}
		
		return sb.toString();
	}
	
	String showContent(VNode node)
	{
		return node.getContentString();
	}
}

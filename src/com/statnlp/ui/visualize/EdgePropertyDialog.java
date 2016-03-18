package com.statnlp.ui.visualize;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import cern.colt.Arrays;

import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ie.linear.IELinearConfig;
import com.statnlp.ui.visualize.type.VLink;

import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class EdgePropertyDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	
	VLink link;
	private JTextField textField_IDarray;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			EdgePropertyDialog dialog = new EdgePropertyDialog(null, null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public EdgePropertyDialog(java.awt.Frame parent, final VLink link) {
		
		super(parent, true);
		this.link = link;
		
		setBounds(100, 100, 280, 131);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel lblWeight = new JLabel("weight:");
			contentPanel.add(lblWeight);
		}
		{
			textField_IDarray = new JTextField();
			contentPanel.add(textField_IDarray);
			textField_IDarray.setColumns(10);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
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
		
		setTitle("VLink: " + link.toString());
	}
	

	
	

}

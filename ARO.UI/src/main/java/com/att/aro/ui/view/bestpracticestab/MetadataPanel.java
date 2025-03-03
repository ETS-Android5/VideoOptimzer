/*
 *  Copyright 2021 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.AroFonts;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.menu.tools.MetadataDialog;

public class MetadataPanel extends AbstractBpPanel {
	private static final long serialVersionUID = 1L;
	
	private JLabel traceStorageLabel = new JLabel();
	private JLabel descriptionLabel = new JLabel();
	private JLabel traceTypeLabel = new JLabel();
	private JLabel deviceOrientationLabel = new JLabel();
	private JLabel targetedAppLabel = new JLabel();
	private JLabel applicationProducerLabel = new JLabel();
	private JLabel traceSourceLabel = new JLabel();
	private JLabel simLabel = new JLabel();
	private JLabel networkLabel = new JLabel();
	private JLabel traceOwnerLabel = new JLabel();
	private JTextArea traceNotes;
	private JPanel textAreaWrapper = new JPanel();
	private Insets insets = new Insets(2, 2, 2, 2);
	private final double weightX = 0.5;
	private IMetaDataHelper metaDataHelper = ContextAware.getAROConfigContext().getBean(IMetaDataHelper.class);

	private MainFrame parent;

	private MetaDataModel metaDataModel;

	private JScrollPane notesScrollPane;

	private JPanel notesScrollPanel;

	private JPanel notesPanel;

	private static final int DEFAULT_WIDTH = 2000;

	/***
	 * Create labels and set font
	 * @param aroView 
	 */
	public MetadataPanel(MainFrame aroView) {
		parent = aroView;
		add(layoutDataPanel(), BorderLayout.NORTH);
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
	}

	@Override
	public int print(Graphics arg0, PageFormat arg1, int arg2) throws PrinterException {
		return 0;
	}

	/***
	 * Add Text to labels and define the attributes
	 */
	@Override
	public JPanel layoutDataPanel() {
		if (dataPanel == null) {
			dataPanel = new JPanel(new GridBagLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY);
			dataPanel.setBackground(Color.WHITE);
			int idx = 0;

			addLabelLineName(traceStorageLabel			, "bestPractices.mdata.traceStorage"		, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(descriptionLabel			, "bestPractices.mdata.description"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(traceTypeLabel				, "bestPractices.mdata.traceType"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(deviceOrientationLabel		, "bestPractices.mdata.deviceOrientation"	, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(targetedAppLabel			, "bestPractices.mdata.targetedApp"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(applicationProducerLabel	, "bestPractices.mdata.applicationProducer" , ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(traceSourceLabel			, "bestPractices.mdata.traceSource"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(simLabel					, "bestPractices.mdata.sim"					, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(networkLabel				, "bestPractices.mdata.network"				, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(traceOwnerLabel			, "bestPractices.mdata.traceOwner"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			
			// Trace Notes textarea
			addTextAreaLineName(++idx, 2, weightX, insets, AroFonts.MONOSPACED);

			traceNotes.setEditable(false);
			traceNotes.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 &&  e.getClickCount() > 1) {
						if (metaDataModel != null) {
							new MetadataDialog(parent, null);
						}
						return;
					}
				}
			});
		}
		return dataPanel;
	}

	private void addLabelLineName(JLabel infoLabel, String labelText, int gridy, int width, double weightx, Insets insets, Font labelFont, Font dataFont) {
		addLabelLineName(infoLabel, labelText, gridy, width, weightx, insets, labelFont);
		infoLabel.setFont(dataFont);
	}

	protected void addTextAreaLineName(int gridy, int width, double weightx, Insets insets, Font dataFont){
		dataPanel.add(getNotesScrollPane(), new GridBagConstraints(0, gridy + 1, width, 1, weightx, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10,2,0,0), 0, 0));
	}

	private JPanel getNotesScrollPane() {

		if (notesScrollPanel == null) {
			
			notesPanel = getNotesPanel();
			notesScrollPane = new JScrollPane(notesPanel);
			notesScrollPane.setMinimumSize(notesPanel.getPreferredSize());
			notesScrollPane.setPreferredSize(notesPanel.getPreferredSize());
			
			notesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			notesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			notesScrollPanel = new JPanel(new BorderLayout());
			JLabel title = new JLabel("Trace Notes");
			title.setFont(AroFonts.SUBHEADER_FONT);
			notesScrollPanel.add(title, BorderLayout.WEST);
			notesScrollPanel.add(notesScrollPane, BorderLayout.SOUTH);
			notesScrollPane.setPreferredSize(new Dimension(840, 220));
			notesScrollPane.setMinimumSize(getPreferredSize());
			// make sure user can see the top line
			traceNotes.setCaretPosition(0);
			
			notesScrollPane.setOpaque(false);
		}
		notesScrollPanel.setMinimumSize(notesScrollPanel.getPreferredSize());
		return notesScrollPanel;
	}
	
	private JPanel getNotesPanel() {

		if (notesPanel == null) {
			notesPanel = new JPanel(new BorderLayout());
			traceNotes = getTextArea(110, AroFonts.MONOSPACED);
			notesPanel.add(traceNotes);
			notesPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, 200));
			return notesPanel;
		}

		return notesPanel;
	}
	
	private JTextArea getTextArea(int fieldSize, Font textFont) {
		if (traceNotes == null) {
			traceNotes = new JTextArea();
			traceNotes.setFont(textFont);
			traceNotes.setColumns(fieldSize);
			traceNotes.setRows(1);
			traceNotes.setLineWrap(true);
			traceNotes.setWrapStyleWord(true);
			traceNotes.setTabSize(4);
			traceNotes.setBackground(Color.WHITE);
		}
		return traceNotes;
	}

	/***
	 * This method is called to check when VO refreshes(Traceload/Open VO) Based
	 * on the flow load the values or empty text
	 */
	@Override
	public void refresh(AROTraceData model) {
		PacketAnalyzerResult analyzerResults = model.getAnalyzerResult();
		AbstractTraceResult traceResults = analyzerResults.getTraceresult();
		
		if (traceResults != null) {
			if (traceResults.getTraceResultType() == TraceResultType.TRACE_DIRECTORY && metaDataHelper != null) {
				try {
					loadMetadataPanel(((TraceDirectoryResult)traceResults).getMetaData());
				} catch (Exception e) {
					clearDirResults();
				}
			} else {
				clearDirResults();
			}
		}
	}

	/***
	 * Loads value from the metadata object
	 * 
	 * @param metaDataModel
	 */
	private void loadMetadataPanel(MetaDataModel metaDataModel) {
		this.metaDataModel = metaDataModel;
		traceStorageLabel.setText(metaDataModel.getTraceStorage());
		descriptionLabel.setText(metaDataModel.getDescription());
		traceTypeLabel.setText(metaDataModel.getTraceType());
		deviceOrientationLabel.setText(metaDataModel.getDeviceOrientation());
		targetedAppLabel.setText(metaDataModel.getTargetedApp());
		applicationProducerLabel.setText(metaDataModel.getApplicationProducer());
		traceSourceLabel.setText(metaDataModel.getTraceSource());
		simLabel.setText(metaDataModel.getSim());
		networkLabel.setText(metaDataModel.getNetWork());
		traceOwnerLabel.setText(metaDataModel.getTraceOwner());		
		traceNotes.setText(metaDataModel.getTraceNotes());
		adjustDimensions();
		textAreaWrapper.setVisible(!metaDataModel.getTraceNotes().isEmpty());
		traceNotes.setEditable(false);
	}

	/**
	 * Adjust internal textArea size inside of the scrollbars
	 */
	private void adjustDimensions() {
		int longestRow = 0;
		int width = DEFAULT_WIDTH;

		FontMetrics fm = traceNotes.getFontMetrics(AroFonts.MONOSPACED);
		int columnCount = 0;
		String[] rows = traceNotes.getText().split(System.lineSeparator());
		int textAreaRows = 1;
		if (rows.length > 0) {
			for (int row = 0; row < rows.length; row++) {
				if (columnCount < rows[row].length()) {
					columnCount = rows[row].length();
					longestRow = row;
				}
			}
			textAreaRows = 4 + StringUtils.countMatches(traceNotes.getText(), System.lineSeparator());
			width = SwingUtilities.computeStringWidth(fm, rows[longestRow]) + 240;
			if (width < DEFAULT_WIDTH) {
				width = DEFAULT_WIDTH;
			}
		}
		
		int height = fm.getHeight();
		height = textAreaRows * height;

		notesPanel.setPreferredSize(new Dimension(width * 2, height * 1));
	}

	/***
	 * Set empty values during VO initial load
	 */
	private void clearDirResults() {	
		traceStorageLabel.setText("");
		descriptionLabel.setText("");
		traceTypeLabel.setText("");
		deviceOrientationLabel.setText("");
		targetedAppLabel.setText("");
		applicationProducerLabel.setText("");
		traceSourceLabel.setText("");
		simLabel.setText("");
		networkLabel.setText("");
		traceNotes.setText("");
		traceOwnerLabel.setText("");
		textAreaWrapper.setVisible(false);
		traceNotes.setEditable(false);
	}
}

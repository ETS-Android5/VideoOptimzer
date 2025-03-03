/*
 *  Copyright 2015 AT&T
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

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.QualityTime;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.bestpractice.AdaptiveBitrateTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BPAdaptiveBitrateTablePanel extends AbstractBpDetailTablePanel {
	private static final long serialVersionUID = 1L;

	public BPAdaptiveBitrateTablePanel() {
		super();
	}

	@Override
	void initTableModel() {
		tableModel = new AdaptiveBitrateTableModel();
	}

	public void setData(Collection<QualityTime> data) {
		setVisible(data != null && !data.isEmpty());
		setScrollSize(MINIMUM_ROWS);
		((AdaptiveBitrateTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	@SuppressWarnings("unchecked")
	public DataTable<QualityTime> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<QualityTime>(tableModel);
			contentTable.setName(ResourceBundleHelper.getMessageString("video.adaptive.bitrate.tableName"));
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
			contentTable.setRowSorter(sorter);
			sorter.toggleSortOrder(AdaptiveBitrateTableModel.COL_2);
			sorter.setComparator(AdaptiveBitrateTableModel.COL_3, Util.getFloatSorter());
			sorter.toggleSortOrder(AdaptiveBitrateTableModel.COL_4);
			sorter.setComparator(AdaptiveBitrateTableModel.COL_5, Util.getFloatSorter());
			sorter.setComparator(AdaptiveBitrateTableModel.COL_6, Util.getFloatSorter());
			sorter.setComparator(AdaptiveBitrateTableModel.COL_7, Util.getFloatSorter());
			sorter.setComparator(AdaptiveBitrateTableModel.COL_8, Util.getFloatSorter());
			
			// set default sort
			sorter.toggleSortOrder(AdaptiveBitrateTableModel.COL_1);

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) contentTable.getPopup();
            popupMenu.initialize();
		}
		return contentTable;
	}
}

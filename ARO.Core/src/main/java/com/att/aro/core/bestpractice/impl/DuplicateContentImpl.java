/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.bestpractice.impl;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.DuplicateContentResult;
import com.att.aro.core.packetanalysis.pojo.CacheAnalysis;
import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;

/**
 * best practice for Duplicate Content Date: November 25, 2014
 */
public class DuplicateContentImpl implements IBestPractice {
	private static final int DUPLICATE_CONTENT_DENOMINATOR = 1000000;

	@Value("${caching.duplicateContent.title}")
	private String overviewTitle;

	@Value("${caching.duplicateContent.detailedTitle}")
	private String detailTitle;

	@Value("${caching.duplicateContent.desc}")
	private String aboutText;

	@Value("${caching.duplicateContent.url}")
	private String learnMoreUrl;

	@Value("${caching.duplicateContent.pass}")
	private String textResultPass;

	@Value("${caching.duplicateContent.results}")
	private String textResults;

	
	@Value("${caching.duplicateContent.excel.results}")
    private String textExcelResults;

	@Value("${exportall.csvPct}")
	private String exportAllPct;

	@Value("${exportall.csvFiles}")
	private String exportAllFiles;

	@Value("${statics.csvUnits.mbytes}")
	private String staticsUnitsMbytes;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		DuplicateContentResult result = new DuplicateContentResult();
		CacheAnalysis cacheAnalysis = tracedata.getCacheAnalysis();
		result.setDuplicateContentBytes(cacheAnalysis.getDuplicateContentBytes());
		result.setDuplicateContentBytesRatio(cacheAnalysis.getDuplicateContentBytesRatio());
		int duplicateContentsize = cacheAnalysis.getDuplicateContent().size();
		result.setDuplicateContentsize(duplicateContentsize);
		long totalTCPBytes = 0;
		if (tracedata.getStatistic() != null) {
			totalTCPBytes = tracedata.getStatistic().getTotalTCPPayloadBytes();
		}
		result.setTotalContentBytes(totalTCPBytes);
		List<CacheEntry> caUResult = cacheAnalysis.getDuplicateContent();
		result.setDuplicateContentList(caUResult);
		int duplicateContentSizeOfUniqueItems = caUResult.size();
		result.setDuplicateContentSizeOfUniqueItems(duplicateContentSizeOfUniqueItems);
		if (duplicateContentsize <= 3) {
			result.setResultType(BPResultType.PASS);
			result.setResultText(textResultPass);
			result.setResultExcelText(BPResultType.PASS.getDescription());
		} else {
			result.setResultType(BPResultType.FAIL);
			DecimalFormat numf = new DecimalFormat("#.##");
			NumberFormat numf2 = NumberFormat.getInstance();
			numf2.setMaximumFractionDigits(3);
			String text = MessageFormat.format(textResults,
					numf.format(result.getDuplicateContentBytes() * 100.0 / result.getTotalContentBytes()),
					result.getDuplicateContentSizeOfUniqueItems(),
					numf2.format(((double) result.getDuplicateContentBytes()) / DUPLICATE_CONTENT_DENOMINATOR),
					numf2.format(((double) result.getTotalContentBytes()) / DUPLICATE_CONTENT_DENOMINATOR));
			result.setResultText(text);

			result.setResultExcelText(
		        MessageFormat.format(textExcelResults, BPResultType.FAIL.getDescription(), result.getDuplicateContentSizeOfUniqueItems(),
			        numf2.format(((double) result.getDuplicateContentBytes()) / DUPLICATE_CONTENT_DENOMINATOR),
                    numf2.format(((double) result.getTotalContentBytes()) / DUPLICATE_CONTENT_DENOMINATOR),
			        numf.format(result.getDuplicateContentBytes() * 100.0 / result.getTotalContentBytes()))
	        );
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
		result.setExportAllFiles(exportAllFiles);
		result.setExportAllPct(exportAllPct);
		result.setStaticsUnitsMbytes(staticsUnitsMbytes);
		return result;
	}
}

/*
 *  Copyright 2017 AT&T
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
package com.att.aro.datacollector.ioscollector.utilities;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.pojo.ErrorCode;
import com.att.aro.core.util.GoogleAnalyticsUtil;

/**
 * error code for ios Collector start from 500
 */
public final class ErrorCodeRegistry {
	private ErrorCodeRegistry(){}
	private static ResourceBundle defaultBundle = ResourceBundle.getBundle("messages");	

	public static ErrorCode getIOSInvalidSudoPassword(){
		ErrorCode error = new ErrorCode();
		error.setCode(500);
		error.setName("Error.validatepassword");
		error.setDescription("There was an error validating password.");
		sendGAErrorCode(error);
		return error;
	}
	public static ErrorCode getTraceDirExist(){
		ErrorCode err = new ErrorCode();
		err.setCode(501);
		err.setName("Found existing trace directory that is not empty");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + 
				" found an existing directory that contains files and did not want to override it. Some files may be hidden.");
		sendGAErrorCode(err);
		return err;
	}
	public static ErrorCode getNoDeviceConnected(){
		ErrorCode err = new ErrorCode();
		err.setCode(502);
		err.setName("No iOS device found.");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + 
				" cannot find any iOS deviced plugged into the machine.");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * failed to create local directory in user's machine to save trace data to.
	 * @return
	 */
	public static ErrorCode getFailedToCreateLocalTraceDirectory(){
		ErrorCode err = new ErrorCode();
		err.setCode(503);
		err.setName("Failed to create local trace directory");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + 
				" tried to create local directory for saving trace data, but failed.");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * failed to find XCode on local machine
	 * @return
	 */
	public static ErrorCode getFailedToLoadXCode(){
		ErrorCode err = new ErrorCode();
		err.setCode(505);
		err.setName("Failed to find XCode on local machine");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + 
				" tried to load XCode on local machine, but failed because XCode is not installed.");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Unsupported XCode Version 
	 * @return
	 */
	public static ErrorCode getUnsupportedXCodeVersion(){
		ErrorCode err = new ErrorCode();
		err.setCode(506);
		err.setName("Unsupported XCode Version");
		err.setDescription(defaultBundle.getString("Error.xcodeversionunsupported"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Incorrect Serial Number 
	 * @return
	 */
	public static ErrorCode getIncorrectSerialNumber(){
		ErrorCode err = new ErrorCode();
		err.setCode(507);
		err.setName("Incorrect Serial Number");
		err.setDescription(defaultBundle.getString("Error.incorrectserialnumber"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * No sudo Password 
	 * @return
	 */
	public static ErrorCode getNoSudoPassword(){
		ErrorCode err = new ErrorCode();
		err.setCode(508);
		err.setName("No sudo Password");
		err.setDescription(MessageFormat.format(defaultBundle.getString("Error.nosudopassword"), 
												ApplicationConfig.getInstance().getAppShortName()));
		sendGAErrorCode(err);
		return err;
	}	
	/**
	 * sudo Password Issue 
	 * @return
	 */
	public static ErrorCode getSudoPasswordIssue(){
		ErrorCode err = new ErrorCode();
		err.setCode(504);
		err.setName("sudo Password Issue");
		err.setDescription(defaultBundle.getString("Error.sudopasswordissue"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Device Info Issue 
	 * @return
	 */
	public static ErrorCode getDeviceInfoIssue(){
		ErrorCode err = new ErrorCode();
		err.setCode(509);
		err.setName("Device Info Issue");
		err.setDescription(defaultBundle.getString("Error.deviceinfoissue"));
		sendGAErrorCode(err);
		return err;
	}	
	/**
	 * Device Version Issue
	 * @return
	 */
	public static ErrorCode getDeviceVersionIssue(){
		ErrorCode err = new ErrorCode();
		err.setCode(510);
		err.setName("Device Version Issue");
		err.setDescription(defaultBundle.getString("Error.deviceversionissue"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Unsupported iOS version 
	 * @return
	 */
	public static ErrorCode getiOSUnsupportedVersion(){
		ErrorCode err = new ErrorCode();
		err.setCode(511);
		err.setName("iOS Unsupported Version");
		err.setDescription(MessageFormat.format(defaultBundle.getString("Error.iosunsupportedversion"), 
												ApplicationConfig.getInstance().getAppShortName()));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Remote virtual interface 
	 * @return
	 */
	public static ErrorCode getrviError(){
		ErrorCode err = new ErrorCode();
		err.setCode(512);
		err.setName("rvi error");
		err.setDescription(defaultBundle.getString("Error.rvierror"));
		sendGAErrorCode(err);
		return err;
	}	
	/**
	 * Missing folder name
	 * @return
	 */
	public static ErrorCode getMissingFolderName(){
		ErrorCode err = new ErrorCode();
		err.setCode(513);
		err.setName("missing folder name");
		err.setDescription(defaultBundle.getString("Error.foldernamerequired"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * Failed to retrieve iOS device data
	 * @return
	 */
	public static ErrorCode getFailedRetrieveDeviceData(String description){
		ErrorCode err = new ErrorCode();
		err.setCode(514);
		err.setName("Failed to retrieve iOS device data");
		err.setDescription(description);
		sendGAErrorCode(err);
		return err;
	}	
	/**
	 * multiple xcode command line tool installed mismatch
	 * @return
	 */
	public static ErrorCode getXCodeCLTError(){
		ErrorCode err = new ErrorCode();
		err.setCode(515);
		err.setName("Multiple Xcode Command Line Tools installed mismatch");
		err.setDescription("You have multiple xcode versions. You will need to configure command-line tools.\nPlease use 'sudo xcode-select -s /PATH/To/Xcode.app'.");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getAppSavingError(){
		ErrorCode err = new ErrorCode();
		err.setCode(516);
		err.setName("Failed to save app to local disk");
		err.setDescription(defaultBundle.getString("Error.app.appsaving"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getRemoveCodeSignatureError(){
		ErrorCode err = new ErrorCode();
		err.setCode(517);
		err.setName("Failed to remove _CodeSignature folder");
		err.setDescription(defaultBundle.getString("Error.app.removecodesignature"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getCreateEntitlementsFileError(){
		ErrorCode err = new ErrorCode();
		err.setCode(518);
		err.setName("Failed to create entitlements.plist for re-signing app");
		err.setDescription(defaultBundle.getString("Error.app.createentitlements"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getRemoveEntitlementsFileError(){
		ErrorCode err = new ErrorCode();
		err.setCode(519);
		err.setName("Failed to remove entitlements.plist");
		err.setDescription(defaultBundle.getString("Error.app.removeentitlements"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getFileUpdateError(String filename){
		ErrorCode err = new ErrorCode();
		err.setCode(520);
		err.setName("Error updating file");
		err.setDescription(defaultBundle.getString("Error.app.updatefile") + " (" + filename +")");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getFileSigningError(){
		ErrorCode err = new ErrorCode();
		err.setCode(521);
		err.setName("Error signing file");
		err.setDescription(defaultBundle.getString("Error.app.filesigning"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getAppDeploymentError(String errorReturn){
		ErrorCode err = new ErrorCode();
		err.setCode(522);
		err.setName("Failed to deploy/launch app");
		err.setDescription(defaultBundle.getString("Error.app.deploymentfailed") + " (" + errorReturn +")");
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getExtractProvPropertyValuesError(){
		ErrorCode err = new ErrorCode();
		err.setCode(523);
		err.setName("Failed to get provisioning profile property values");
		err.setDescription(defaultBundle.getString("Error.app.getprovpropsfailed"));
		sendGAErrorCode(err);
		return err;
	}	
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getProvProfileileExpiredError(){
		ErrorCode err = new ErrorCode();
		err.setCode(524);
		err.setName("Provisioning profile expired");
		err.setDescription(defaultBundle.getString("Error.app.provprofileexpired"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getAppUnzipError(){
		ErrorCode err = new ErrorCode();
		err.setCode(525);
		err.setName("Error unzipping app");
		err.setDescription(defaultBundle.getString("Error.app.unzipapp"));
		sendGAErrorCode(err);
		return err;
	}
	/**
	 * 
	 * @return
	 */
	public static ErrorCode getAppTrustError(){
		ErrorCode err = new ErrorCode();
		err.setCode(526);
		err.setName("App trust error");
		err.setDescription(defaultBundle.getString("Error.app.trust"));
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getImageDecoderError(String message) {
		ErrorCode err = new ErrorCode();
		err.setCode(527);
		err.setName("ImageDecoder Error");
		err.setDescription(message);
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getDumpcapError() {
		ErrorCode err = new ErrorCode();
		err.setCode(528);
		err.setName("Dumpcap Error");
		err.setDescription(defaultBundle.getString("Error.app.dumpcap"));
		sendGAErrorCode(err);
		return err;
	}

	/**
	 * RVI Drop Issue
	 * @return
	 */
	public static ErrorCode getRVIDropIssue(){
		ErrorCode err = new ErrorCode();
		err.setCode(529);
		err.setName("RVI Issue. XCode dropped the RVI connection ");
		err.setDescription(defaultBundle.getString("Error.rvidropissue"));
		sendGAErrorCode(err);
		return err;
	}
	
	private static void sendGAErrorCode(ErrorCode err){
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendErrorEvents(err.getName(),err.getDescription(), false);
	}
}

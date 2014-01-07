/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
/**
 * 
 */
package org.ebayopensource.turmeric.eclipse.services.ui.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import org.ebayopensource.turmeric.eclipse.core.logging.SOALogger;
import org.ebayopensource.turmeric.eclipse.core.model.consumer.ConsumerFromWsdlParamModel;
import org.ebayopensource.turmeric.eclipse.core.resources.constants.SOAProjectConstants;
import org.ebayopensource.turmeric.eclipse.exception.resources.projects.SOAConsumerCreationFailedException;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.GlobalRepositorySystem;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.TrackingEvent;
import org.ebayopensource.turmeric.eclipse.resources.util.SOAServiceUtil;
import org.ebayopensource.turmeric.eclipse.services.buildsystem.ServiceCreator;
import org.ebayopensource.turmeric.eclipse.services.ui.wizards.pages.ConsumerFromExistingWSDLWizardPage;
import org.ebayopensource.turmeric.eclipse.ui.AbstractSOADomainWizard;
import org.ebayopensource.turmeric.eclipse.ui.SOABasePage;
import org.ebayopensource.turmeric.eclipse.utils.plugin.ProgressUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.WorkspaceUtil;
import org.ebayopensource.turmeric.eclipse.utils.ui.UIUtil;
import org.ebayopensource.turmeric.eclipse.validator.core.ISOAPreValidator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;

/**
 * The Class ConsumerFromWSDLWizard.
 *
 * @author yayu
 */
public class ConsumerFromWSDLWizard extends AbstractSOADomainWizard {
	private ConsumerFromExistingWSDLWizardPage consumerFromWsdl = null;
	
	private static final SOALogger logger = SOALogger.getLogger();

	/**
	 * Instantiates a new consumer from wsdl wizard.
	 */
	public ConsumerFromWSDLWizard() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IWizardPage[] getContentPages() {
		consumerFromWsdl = new ConsumerFromExistingWSDLWizardPage(
				getSelection());
		//intfDependenciesPage = new DependenciesWizardPage(SOAMessages.SVC_INTF);
		List<IWizardPage> pages = new ArrayList<IWizardPage>();
		
		pages.add(consumerFromWsdl);
		//pages.add(intfDependenciesPage);
		
		return pages.toArray(new IWizardPage[pages.size()]);
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object getCreatingType() {
		return ISOAPreValidator.CONSUMER_FROM_WSDL;
	}
	
	
	public class Bundle {

		private String version;

		private String bundleName;

		private String uri;

		private String severity;

		public String getSeverity() {
			return severity;
		}

		public void setSeverity(String severity) {
			this.severity = severity;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		

		public String getBundleName() {
			return bundleName;
		}

		public void setBundleName(String bundleName) {
			this.bundleName = bundleName;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

	}
	
	@Override
	public boolean canFinish() {
		if(getContainer().getCurrentPage() == consumerFromWsdl){
			if((super.canFinish())&&(consumerFromWsdl.canFlipToNextPage()==false))
			return true;
			return false;
		}
		return true;
		
	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 * @throws HttpException 
	 * @throws JSONException 
	 */
	
	
	
	
	@Override
	public boolean performFinish() {
		
		
		// saving the user selected project dir
		if (SOALogger.DEBUG)
			logger.entering();
		
		
		final boolean overrideWorkspaceRoot = consumerFromWsdl
				.isOverrideProjectRootDirectory();
		final String workspaceRootDirectory = consumerFromWsdl
				.getProjectRootDirectory();
		if (overrideWorkspaceRoot)
			SOABasePage.saveWorkspaceRoot(workspaceRootDirectory);

		final String serviceName = consumerFromWsdl.getAdminName();
		final String servicePackage = consumerFromWsdl.getServicePackage();
		Map<String,String> allNSToPackMappings= consumerFromWsdl.getNamespaceToPackageMappings();
		allNSToPackMappings.put(servicePackage.toLowerCase(),servicePackage.toLowerCase());
		allNSToPackMappings.put(servicePackage.toLowerCase()+".gen",servicePackage.toLowerCase()+".gen");
		String serviceInterface = StringUtils.isBlank(servicePackage) ? serviceName
				: servicePackage + SOAProjectConstants.CLASS_NAME_SEPARATOR
						+ serviceName;

		//During creation service Name is hardcoded
		String fullServiceName = "com.ebay.soa.interface."+serviceName;
		
		Set<String> mappedPackages = new HashSet<String>();
		mappedPackages.addAll(allNSToPackMappings.values());
		
		
		//Flag to let the wsdl field focus lost listener know that OK button has been pressed
		//To avoid the https:// message box from reappearing once the focus is lost
		//Can be used by other events too
		consumerFromWsdl.done=1;
		//Will get the type library artifacts to be updated here and add:
		
		final ConsumerFromWsdlParamModel uiModel = new ConsumerFromWsdlParamModel();
		uiModel.setServiceName(serviceName);
		uiModel.setPublicServiceName(consumerFromWsdl.getPublicServiceName());
		uiModel.setServiceInterface(serviceInterface);
		uiModel.setOverrideWorkspaceRoot(overrideWorkspaceRoot);
		uiModel.setWorkspaceRootDirectory(workspaceRootDirectory);
		uiModel.setServiceImpl(consumerFromWsdl
				.getFullyQualifiedServiceImplementation());
		if(!StringUtils.isEmpty(consumerFromWsdl.getServiceVersion()))
		uiModel.setServiceVersion(consumerFromWsdl.getServiceVersion());
		else{
			uiModel.setServiceVersion("1.0.0");
		}
		//adding a line here to set the version as 1.0.0 if empty to fix cons from wsdl flow, as the text box is now missing in the new wizard..
		uiModel.setServiceLayer(consumerFromWsdl.getServiceLayer());
		final String clientName = consumerFromWsdl.getClientName();
		uiModel
				.setWSDLSourceType(SOAProjectConstants.InterfaceWsdlSourceType.EXISTIING);
		uiModel.setNamespaceToPacakgeMappings(consumerFromWsdl
				.getNamespaceToPackageMappings());
		uiModel.setClientName(clientName);
		if (StringUtils.isNotBlank(consumerFromWsdl.getConsumerId()))
			uiModel.setConsumerId(consumerFromWsdl.getConsumerId());
		uiModel.setEnvironments(consumerFromWsdl.getEnvironments());
		uiModel.setServiceDomain(consumerFromWsdl.getServiceDomain());
		uiModel.setNamespacePart(consumerFromWsdl.getDomainClassifier());
		uiModel.setTypeFolding(consumerFromWsdl.getTypeFolding());
		//Adding library dependencies
	
		try {
			uiModel.setOriginalWsdlUrl(new URL(consumerFromWsdl.getWSDLURL()));
			final WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {

				@Override
				protected void execute(IProgressMonitor monitor)
						throws CoreException, InvocationTargetException,
						InterruptedException {
					final long startTime = System.currentTimeMillis();
					final int totalWork = ProgressUtil.PROGRESS_STEP * 30;
					monitor.beginTask("Creating consumer from WSDL->"
							+ clientName, totalWork);
					ProgressUtil.progressOneStep(monitor);
					
					try {
						ServiceCreator.createConsumerFromExistingWSDL(uiModel,
								monitor);
						//PerformSplitPackageValidation Again
					
						// we should open the wsdl file for any successful
						// creation of an interface project.
						final IFile wsdlFile = SOAServiceUtil
								.getWsdlFile(serviceName);
						WorkspaceUtil.refresh(wsdlFile, monitor);
						if (wsdlFile.exists()) {
							IDE.openEditor(
									UIUtil.getWorkbench()
											.getActiveWorkbenchWindow()
											.getActivePage(), wsdlFile);
						}
						
						final TrackingEvent event = new TrackingEvent(
								"NewConsumerFromWSDL", new Date(startTime),
								System.currentTimeMillis() - startTime);
						GlobalRepositorySystem.instanceOf()
								.getActiveRepositorySystem().trackingUsage(
										event);
					} catch (Exception e) {
						logger.error(e);
						throw new SOAConsumerCreationFailedException(
								"Failed to create consumer from WSDL->"
										+ clientName, e);
					} finally {
						monitor.done();
						
					}
				}

			};
			getContainer().run(false, true, operation);
			changePerspective();
		} catch (Exception e) {
			logger.error(e);
			UIUtil.showErrorDialog(getShell(),
					"Error Occured During Consumer Creation", null, e);
			if (SOALogger.DEBUG)
				logger.exiting(false);
			return false;
		}
		if (SOALogger.DEBUG)
			logger.exiting(true);
		
		return true;
	}

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMinimumHeight() {
		return super.getMinimumHeight() - 100;
	}
}

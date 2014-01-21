/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.services.ui.wizards.pages;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;

import org.apache.commons.lang.StringUtils;
import org.ebayopensource.turmeric.eclipse.config.exception.SOAConfigAreaCorruptedException;
import org.ebayopensource.turmeric.eclipse.config.repo.SOAConfigExtensionFactory;
import org.ebayopensource.turmeric.eclipse.config.repo.SOAConfigExtensionFactory.SOAConfigTemplate;
import org.ebayopensource.turmeric.eclipse.core.logging.SOALogger;
import org.ebayopensource.turmeric.eclipse.core.resources.constants.SOAProjectConstants;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.GlobalRepositorySystem;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.ISOAHelpProvider;
import org.ebayopensource.turmeric.eclipse.services.resources.SOAConstants;
import org.ebayopensource.turmeric.eclipse.services.ui.SOAMessages;
import org.ebayopensource.turmeric.eclipse.soatools.configtool.ConfigTool;
import org.ebayopensource.turmeric.eclipse.ui.wizards.pages.AbstractNewServiceFromWSDLWizardPage;
import org.ebayopensource.turmeric.eclipse.utils.plugin.EclipseMessageUtils;
import org.ebayopensource.turmeric.eclipse.utils.plugin.WorkspaceUtil;
import org.ebayopensource.turmeric.eclipse.utils.ui.UIUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;


/**
 * Wizard for Service from template WSDL option. Here user decided to create the
 * service from template WSDL. This page takes all the initial inputs like the
 * template, the service name, name space, location, type folding, common types
 * name space etc. In short some of this information goes to the WSDL as part of
 * the templating process and some of them goes to the properties file and the
 * xml files. The SOA specific instant validation also will find a place in this
 * page as we show as much issues then and there in the UI itself, so that user
 * gets an early feed back. Additionally adds a context help id also for easy
 * access from this page itself.
 * 
 * @author smathew
 * 
 * 
 * 
 */
public class ServiceFromNewWSDLPage extends
		AbstractNewServiceFromWSDLWizardPage {
private CCombo templateFileCombo;

private Label platformParentNote;

private Label platformParentNote2;
	
	private static final SOALogger logger = SOALogger.getLogger();

	/**
	 * Instantiates a new service from new wsdl page.
	 */
	public ServiceFromNewWSDLPage() {
		super(SOAConstants.SVC_PAGE_NAME, SOAMessages.NEW_SVC_TITLE,
				SOAMessages.NEW_SVC_DESC);
	}
	
	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.AbstractSOAProjectWizardPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible == true) {
			dialogChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createControl(final Composite parent) {
		try {
			final Composite container = super.createParentControl(parent, 4);
			addWorkspaceRootChooser(container);
			addServiceDomainList(container);
			addServiceVersion(container).addModifyListener(modifyListener);
			addTargetNamespace(container, populateServiceNamespace(), true);
			
			addServiceName(container, true).setText(getDefaultResourceName());
			addAdminName(container).setText(computeServiceName());
			addTypeNamespace(container).setText(getTargetNamespace());
			
			addServicePackage(container);
			addServiceImpl(container);
			addServiceLayer(container);
			addTemplateFileComboBox(container);
			createServiceImplTypeCombo(container);
			addTypeFolding(container);
			addPlatFormParentExplanation(container);
			modifyListener.modifyText(null);
		} catch (Exception e) {
			logger.error(e);
			UIUtil.showErrorDialog(e);
		}
	}

	private void addPlatFormParentExplanation(Composite container) {
		String RPVersion = WorkspaceUtil.getRPVersion();
		if((RPVersion!=null)||(RPVersion.equalsIgnoreCase("")))
				{
					platformParentNote = new Label(container, SWT.LEFT);
					Color color = new Color(Display.getDefault(), 200,111,50);
					platformParentNote.setForeground(color);
					platformParentNote.setText("**Using RaptorPlatform Version :"+ RPVersion);
					GridData gridData = new GridData();
					gridData.horizontalSpan = 3;
					platformParentNote.setLayoutData(gridData);
					
					platformParentNote2 = new Label(container, SWT.LEFT);
					platformParentNote2.setForeground(color);
					platformParentNote2.setText("**To modify RaptorPlatform, update raptorSoa.properties at "+ WorkspaceUtil.getRaptorSoaPropertiesLocation());
					platformParentNote2.setLayoutData(gridData);
				}
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void targetNamespaceModified(String newNamespace) {
		super.targetNamespaceModified(newNamespace);
		if (typeNamespaceText != null && 
				StringUtils.isNotBlank(newNamespace)) {
			typeNamespaceText.setText(newNamespace);
		}
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.SOABasePage#getHelpContextID()
	 */
	@Override
	public String getHelpContextID() {
		return GlobalRepositorySystem
				.instanceOf()
				.getActiveRepositorySystem()
				.getHelpProvider()
				.getHelpContextID(
						ISOAHelpProvider.PAGE_CREATE_SERVICE_FROM_TEMPLATE_WSDL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.wizards.pages.AbstractNewServiceWizardPage#getDefaultTypeNamespace()
	 */
	@Override
	public String getDefaultTypeNamespace() {
		return populateServiceNamespace();
	}

	/**
	 * Adds the template file combo box.
	 *
	 * @param parent the parent
	 * @throws SOAConfigAreaCorruptedException the sOA config area corrupted exception
	 */
	protected void addTemplateFileComboBox(final Composite parent)
			throws SOAConfigAreaCorruptedException {
		final String org = getOrganizationProvider().getName();
		final List<String> templates = new ArrayList<String>();
		for (SOAConfigTemplate template : SOAConfigExtensionFactory.getWSDLTemplates(org)) {
			templates.add(template.getName());
		}
		
		templateFileCombo = super.createCCombo(parent, SOAMessages.TMPLT_WSDL,
				false, templates.toArray(new String[0]), "the template for the new service WSDL");
		if (templates != null && templates.isEmpty() == false) {
			templateFileCombo.select(0);
		}
		/*if (files != null && files.length > 0) {
			templateFileCombo.select(0);
		}*/
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.wizards.pages.AbstractNewServiceFromWSDLWizardPage#dialogChanged()
	 */
	@Override
	protected boolean dialogChanged() {
		if (super.dialogChanged() == false)
			return false;

		if (templateFileCombo != null && 
				templateFileCombo.getSelectionIndex() == -1) {
			updateStatus(this.templateFileCombo, SOAMessages.TEMPLATE_ERR);
			return false;
		}
		if (StringUtils.isNotEmpty(getResourceName())
				&& Character.isLowerCase(getResourceName().charAt(0))) {
			updatePageStatus(getResourceNameText(), 
					EclipseMessageUtils.createStatus(
					SOAMessages.SVCNAME_ERR, IStatus.WARNING));
			return true;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.wizards.pages.AbstractNewServiceFromWSDLWizardPage#dialogChanged(boolean)
	 */
	@Override
	public boolean dialogChanged(boolean validateWsdl) {
		if (super.dialogChanged(validateWsdl) == false)
			return false;

		return true;
	}

	/**
	 * Gets the template file.
	 *
	 * @return the template file
	 * @throws SOAConfigAreaCorruptedException the sOA config area corrupted exception
	 */
	public URL getTemplateFile() throws SOAConfigAreaCorruptedException {
		final String templateFileName = getTextValue(templateFileCombo);
		final String org = getOrganizationProvider().getName();
		return SOAConfigExtensionFactory.getWSDLTemplate(org, templateFileName);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.AbstractSOAProjectWizardPage#getProjectNames()
	 */
	@Override
	public List<ProjectNameControl> getProjectNames() {
		final List<ProjectNameControl> result = new ArrayList<ProjectNameControl>(2);
		final List<Control> controls = new ArrayList<Control>(5);
		controls.add(getResourceNameText());
		if (this.overrideAdminNameButton != null 
				&& this.overrideAdminNameButton.getSelection() == false) {
			controls.add(this.publicServiceNameText);
			controls.add(this.resourceVersionText);
			if (this.serviceDomainList != null) {
				controls.add(this.serviceDomainList);
			}
		}
		result.add(new ProjectNameControl(
				getResourceName(), 
				controls));
		result.add(new ProjectNameControl(
				getResourceName() + SOAProjectConstants.IMPL_PROJECT_SUFFIX, 
				controls));
		return result;
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.eclipse.ui.wizards.pages.AbstractNewServiceFromWSDLWizardPage#wsdlChanged(javax.wsdl.Definition)
	 */
	@Override
	public Map<String, String> getNamespaceToPackageMappings() {
		Map<String, String> result = new LinkedHashMap<String, String>();
		String targetNs = getTargetNamespace();
		String typeNs = getTypeNamespace();
		result.put(targetNs, ConfigTool
				.getTypePackageNameFromNamespace(targetNs, getPublicServiceName()));
		if (StringUtils.isNotBlank(typeNs) && targetNs.equals(typeNs) == false) {
			result.put(typeNs, ConfigTool
					.getTypePackageNameFromNamespace(typeNs, getPublicServiceName()));
		}
		return result;
	}

	@Override
	public void wsdlChanged(Definition wsdl) {

	}
}

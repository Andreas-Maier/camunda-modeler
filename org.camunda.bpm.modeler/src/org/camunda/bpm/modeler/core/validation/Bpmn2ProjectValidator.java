/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.camunda.bpm.modeler.core.validation;

import java.io.IOException;

import org.camunda.bpm.modeler.Messages;
import org.camunda.bpm.modeler.core.Activator;
import org.camunda.bpm.modeler.core.ProxyURIConverterImplExtension;
import org.camunda.bpm.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.camunda.bpm.modeler.core.model.Bpmn2ModelerResourceSetImpl;
import org.camunda.bpm.modeler.core.preferences.Bpmn2Preferences;
import org.camunda.bpm.modeler.core.project.Bpmn2Nature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.validation.marker.MarkerUtil;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.model.IConstraintStatus;
import org.eclipse.emf.validation.service.IBatchValidator;
import org.eclipse.emf.validation.service.ModelValidationService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.ValidatorMessage;

public class Bpmn2ProjectValidator extends AbstractValidator {

	/** ID for BPMN2 specific problem markers. */
	public static final String BPMN2_MARKER_ID = "org.camunda.bpm.modeler.problemMarker"; //$NON-NLS-1$

	@Override
	public ValidationResult validate(final ValidationEvent event, final ValidationState state, final IProgressMonitor monitor) {

		ValidationResult result = new ValidationResult();
		
		IResource r = event.getResource();
		if (isFileRemoved(event) || isDerived(r) || !(r instanceof IFile)) {
			return result;
		}
		
		IFile file = (IFile) r;
		
		deleteValidationMarkers(file);

		ResourceSet rs = new Bpmn2ModelerResourceSetImpl();
		rs.setURIConverter(new ProxyURIConverterImplExtension());

		Resource resource = rs.createResource(URI.createPlatformResourceURI(file.getFullPath().toString(), true),
				Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID);
		
		try {
			resource.load(null);
		
			if (resource.getContents().isEmpty()) {
				result.add(createValidatorMessage(Messages.Bpmn2ProjectValidator_1, file));
			} else {
				IBatchValidator validator = ModelValidationService.getInstance().newValidator(EvaluationMode.BATCH);
				processStatus(validator.validate(resource.getContents(), monitor), file, result);
			}
		} catch (IOException e) {
			result.add(createValidatorMessage(Messages.Bpmn2ProjectValidator_2, file));
		}
		
		return result;
	}

	private ValidatorMessage createValidatorMessage(final String message, final IFile file) {
		ValidatorMessage m = ValidatorMessage.create(message, file);
		m.setType(BPMN2_MARKER_ID);
		return m;
	}

	private void deleteValidationMarkers(final IFile file) {
		try {
			// delete validation markers
			file.deleteMarkers(BPMN2_MARKER_ID, false, IProject.DEPTH_INFINITE);
		} catch (CoreException e) {
			Activator.logStatus(new Status(Status.WARNING, Activator.PLUGIN_ID, Messages.Bpmn2ProjectValidator_3, e));
		}
	}

	private boolean isDerived(final IResource file) {
		return file.isDerived(IResource.CHECK_ANCESTORS);
	}

	private boolean isFileRemoved(final ValidationEvent event) {
		return (event.getKind() & IResourceDelta.REMOVED) != 0;
	}

	/**
	 * Validates the given resource
	 * 
	 * @param resource
	 * @param monitor to report progress to
	 * 
	 * @return
	 */
	public static ValidationResult validate(final IResource resource, final IProgressMonitor monitor) {
		if (!isBPMN2File(resource)) {
			return null;
		}
		
		Validator[] validators = ValidationFramework.getDefault().getValidators();
		for (Validator v : validators) {
			if (Bpmn2ProjectValidator.class.getName().equals(v.getValidatorClassname())) {
				return v.validate(resource, IResourceDelta.CHANGED, null, monitor);
			}
		}
		
		return null;
	}

	public static void validate(final IResourceDelta delta, final IProgressMonitor monitor) {
		IResource resource = delta.getResource();
		if (isBPMN2File(resource)) {
			Validator[] validators = ValidationFramework.getDefault().getValidatorsFor(resource);
			for (Validator v : validators) {
				if (Bpmn2ProjectValidator.class.getName().equals(v.getValidatorClassname())) {
					v.validate(resource, delta.getKind(), null, monitor);
					break;
				}
			}
		}
	}

	public static boolean isBPMN2File(final IResource resource) {
		if (resource instanceof IFile) {
			try {
				IContentDescription cd = ((IFile) resource).getContentDescription();
				if (cd != null) {
					return Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID.equals(cd.getContentType().getId());
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	public static ValidationResult validateResource(final Resource resource, final IProgressMonitor monitor) {
		IFile file = getFile(resource);
		
		return validate(file, monitor);
	}
	
	public static boolean validateAfterSave(final Resource resource, final IProgressMonitor monitor) {

		boolean needValidation = false;
		
		IFile file = getFile(resource);
		
		IProject project = file.getProject();

		if (project != null) {
			try {
				IProjectNature nature = project.getNature(Bpmn2Nature.NATURE_ID);
				if (nature == null) {
					Bpmn2Preferences preferences = Bpmn2Preferences.getInstance(project);
					if (preferences.getCheckProjectNature()) {
						Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						String title = Messages.Bpmn2ProjectValidator_4;
						String message = Messages.Bpmn2ProjectValidator_5 + project.getName()
								+ Messages.Bpmn2ProjectValidator_6
								+ Messages.Bpmn2ProjectValidator_7
								+ Messages.Bpmn2ProjectValidator_8
								+ Messages.Bpmn2ProjectValidator_9;
						MessageDialogWithToggle result = MessageDialogWithToggle.open(MessageDialog.QUESTION, shell, title,
								message, Messages.Bpmn2ProjectValidator_10, // toggle message
								false, // toggle state
								null, // pref store
								null, // pref key
								SWT.NONE);
						if (result.getReturnCode() == IDialogConstants.YES_ID) {
							IProjectDescription description = project.getDescription();
							String[] natures = description.getNatureIds();
							String[] newNatures = new String[natures.length + 1];
							System.arraycopy(natures, 0, newNatures, 0, natures.length);
							newNatures[natures.length] = Bpmn2Nature.NATURE_ID;
							description.setNatureIds(newNatures);
							project.setDescription(description, null);
							needValidation = true;
						}
						if (result.getToggleState()) {
							// don't ask again
							preferences.setCheckProjectNature(false);
						}
					}
				} else
					needValidation = true;

			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		if (needValidation) {
			validate(file, monitor);
			return true;
		}

		return false;
	}

	private static IFile getFile(final Resource resource) {
		String pathString = resource.getURI().toPlatformString(true);
		IPath path = Path.fromOSString(pathString);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}

	public static void processStatus(final IStatus status, final IResource resource, final ValidationResult result) {
		if (status.isMultiStatus()) {
			for (IStatus child : status.getChildren()) {
				processStatus(child, resource, result);
			}
		} else if (!status.isOK()) {
			result.add(createValidationMessage(status, resource));
		}
	}

	public static ValidatorMessage createValidationMessage(final IStatus status, final IResource resource) {
		ValidatorMessage message = ValidatorMessage.create(status.getMessage(), resource);
		switch (status.getSeverity()) {
		case IStatus.INFO:
			message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			break;
		case IStatus.WARNING:
			message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			break;
		case IStatus.ERROR:
		case IStatus.CANCEL:
			message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			break;
		}

		if (status instanceof IConstraintStatus) {
			IConstraintStatus ics = (IConstraintStatus) status;
			message.setAttribute(EValidator.URI_ATTRIBUTE, EcoreUtil.getURI(ics.getTarget()).toString());
			message.setAttribute(MarkerUtil.RULE_ATTRIBUTE, ics.getConstraint().getDescriptor().getId());
			if (ics.getResultLocus().size() > 0) {
				StringBuffer relatedUris = new StringBuffer();
				for (EObject eobject : ics.getResultLocus()) {
					relatedUris.append(EcoreUtil.getURI(eobject).toString()).append(" "); //$NON-NLS-1$
				}
				relatedUris.deleteCharAt(relatedUris.length() - 1);
				String uris = relatedUris.toString();
				message.setAttribute(EValidator.RELATED_URIS_ATTRIBUTE, uris);
			}
		}

		message.setType(BPMN2_MARKER_ID);

		return message;
	}

	@Override
	public void clean(final IProject project, final ValidationState state, final IProgressMonitor monitor) {
		super.clean(project, state, monitor);
		try {
			project.deleteMarkers(BPMN2_MARKER_ID, false, IProject.DEPTH_INFINITE);
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(e.getStatus());
		}
	}
}

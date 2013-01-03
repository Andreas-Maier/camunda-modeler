package org.eclipse.bpmn2.modeler.ui.property.tabs.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.change.filter.FeatureChangeFilter;
import org.eclipse.bpmn2.modeler.ui.change.filter.NestedFeatureChangeFilter;
import org.eclipse.bpmn2.modeler.ui.property.tabs.binding.BaseElementIdComboBinding;
import org.eclipse.bpmn2.modeler.ui.property.tabs.binding.change.EAttributeChangeSupport;
import org.eclipse.bpmn2.modeler.ui.property.tabs.binding.change.EObjectChangeSupport.ModelChangedEvent;
import org.eclipse.bpmn2.modeler.ui.property.tabs.util.Events;
import org.eclipse.bpmn2.modeler.ui.property.tabs.util.HelpText;
import org.eclipse.bpmn2.modeler.ui.property.tabs.util.PropertyUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Builder for the error code / name property.
 * 
 * @author nico.rehwaldt
 */
public class DefinitionReferencePropertyBuilder<T extends RootElement> extends AbstractPropertiesBuilder<EventDefinition> {

	private final EStructuralFeature ROOT_ELEMENTS_FEATURE = Bpmn2Package.eINSTANCE.getDefinitions_RootElements();

	private final EStructuralFeature refFeature;
	private final EStructuralFeature nameFeature;
	
	private final String label;

	private final Class<T> definitionCls;

	private String note;

	public DefinitionReferencePropertyBuilder(
			Composite parent, GFPropertySection section, EventDefinition bo, 
			String label, EStructuralFeature refFeature, EStructuralFeature nameFeature, Class<T> definitionCls) {
		
		super(parent, section, bo);
		
		this.refFeature = refFeature;
		this.nameFeature = nameFeature;
		
		this.definitionCls = definitionCls;
		
		this.label = label;
		this.note = String.format(HelpText.ELEMENT_DEF_TABLE, new Object[] { definitionCls.getSimpleName() });
	}
	
	@Override
	public void create() {

		final CCombo dropDown = PropertyUtil.createDropDown(section, parent, label);
		
		if (note != null) {
			PropertyUtil.attachNote(dropDown, note);
		}
		
		Definitions definitions = ModelUtil.getDefinitions(bo);
		
		// register change support
		EAttributeChangeSupport changeSupport = new EAttributeChangeSupport(definitions, ROOT_ELEMENTS_FEATURE, dropDown);
		changeSupport.setFilter(new FeatureChangeFilter(definitions, ROOT_ELEMENTS_FEATURE).or(new NestedFeatureChangeFilter(definitions, ROOT_ELEMENTS_FEATURE)));
		
		changeSupport.register();
		
		dropDown.addListener(Events.MODEL_CHANGED, new Listener() {
			
			@Override
			public void handleEvent(Event e) {
				ModelChangedEvent event = (ModelChangedEvent) e;
				if (ROOT_ELEMENTS_FEATURE.equals(event.getFeature())) {
					updateDropdownLabels(dropDown);
				}
			}
		});
		
		BaseElementIdComboBinding<T> modelViewBinding = new BaseElementIdComboBinding<T>(bo, refFeature, dropDown) {

			@Override
			protected T getModelById(String id) {
				return getDefinitionByLabel(id);
			}

			@Override
			protected String toString(T value) {
				if (value == null) {
					return "";
				} else {
					return getDefinitionLabel(value);
				}
			}
		};
		
		modelViewBinding.establish();
		
		updateDropdownLabels(dropDown);
	}

	/**
	 * Returns the error definition with the given id
	 * in the context of this factory
	 * 
	 * @param id
	 * @return
	 */
	private T getDefinitionByLabel(String id) {
		List<T> definitions = getDefinitions();
		for (T d: definitions) {
			String nodeId = getDefinitionLabel(d);
			if (nodeId.equals(id)) {
				return d;
			}
		}
		
		return null;
	}

	private List<T> getDefinitions() {
		Definitions definitions = ModelUtil.getDefinitions(bo);
		return ModelUtil.getAllRootElements(definitions, definitionCls);
	}
	
	private void updateDropdownLabels(CCombo dropDown) {

		List<T> definitions = getDefinitions();
		List<String> definitionLabels = new ArrayList<String>();
		
		T selectedDefinition = (T) bo.eGet(refFeature);
		
		definitionLabels.add("");
		
		for (T d: definitions) {
			definitionLabels.add(getDefinitionLabel(d));
		}
		
		for (String s: dropDown.getItems()) {
			dropDown.remove(s);
		}
		
		for (String s: definitionLabels) {
			dropDown.add(s);
		}
		
		if (selectedDefinition != null) {
			dropDown.setText(getDefinitionLabel(selectedDefinition));
		}
	}
	
	private String getDefinitionLabel(T d) {
		return d.eGet(nameFeature) + "(" + d.getId() + ")";
	}
}

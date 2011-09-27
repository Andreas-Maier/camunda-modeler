/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.property;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.bpmn2.modeler.ui.property.providers.ColumnTableProvider;
import org.eclipse.bpmn2.modeler.ui.property.providers.TableCursor;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap.Entry;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.emf.edit.ui.provider.PropertyDescriptor.EDataTypeCellEditor;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.ResourceSetChangeEvent;
import org.eclipse.emf.transaction.ResourceSetListenerImpl;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.impl.TransactionChangeRecorder;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
 

/**
 * @author Bob Brodt
 *
 */
public class AbstractBpmn2TableComposite extends Composite {

	public static final Bpmn2Factory MODEL_FACTORY = Bpmn2Factory.eINSTANCE;
	
	public static final int HIDE_TITLE = 1 << 18; // Hide section title - useful if this is the only thing in the PropertySheetTab
	public static final int ADD_BUTTON = 1 << 19; // show "Add" button
	public static final int REMOVE_BUTTON = 1 << 20; // show "Remove" button
	public static final int MOVE_BUTTONS = 1 << 21; // show "Up" and "Down" buttons
	public static final int EDIT_BUTTON = 1 << 23; // show "Edit..." button
	public static final int SHOW_DETAILS = 1 << 24; // create a "Details" section
	public static final int DEFAULT_STYLE = (
			ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|SHOW_DETAILS);
	
	public static final int CUSTOM_STYLES_MASK = (
			HIDE_TITLE|ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|EDIT_BUTTON|SHOW_DETAILS);
	public static final int CUSTOM_BUTTONS_MASK = (
			ADD_BUTTON|REMOVE_BUTTON|MOVE_BUTTONS|EDIT_BUTTON);

	protected TrackingFormToolkit toolkit;
	protected BPMN2Editor bpmn2Editor;
	protected TabbedPropertySheetPage tabbedPropertySheetPage;

	// widgets
	SashForm sashForm;
	Section tableSection;
	Section detailSection;
	
	Table table;
	TableViewer tableViewer;
	
	Composite tableAndButtonsComposite;
	Composite buttonsComposite;
	Composite tableComposite;
	Composite detailComposite;
	
	Button addButton;
	Button removeButton;
	Button upButton;
	Button downButton;
	Button editButton;

	protected int style;
	
	protected AbstractTableProvider tableProvider;
	
	public AbstractBpmn2TableComposite(final Composite parent, int style) {
		super(parent, style & ~CUSTOM_STYLES_MASK);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (!(parent instanceof AbstractBpmn2PropertiesComposite))
					toolkit.dispose();
			}
		});
		if (parent instanceof AbstractBpmn2PropertiesComposite) {
			toolkit = ((AbstractBpmn2PropertiesComposite)parent).getToolkit();
		}
		else {
			toolkit = new TrackingFormToolkit(Display.getCurrent());
		}
		this.style = style;
		toolkit.track(this);
		toolkit.paintBordersFor(this);
		setLayout(new GridLayout(3, false));
		// assume we are being placed in an AbstractBpmn2PropertyComposite which has
		// a GridLayout of 3 columns
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		if (parent instanceof AbstractBpmn2PropertiesComposite) {
			bpmn2Editor = ((AbstractBpmn2PropertiesComposite)parent).getDiagramEditor();
			tabbedPropertySheetPage = ((AbstractBpmn2PropertiesComposite)parent).getSheetPage();
		}
	}
	
	public void setSheetPage(TabbedPropertySheetPage tabbedPropertySheetPage) {
		this.tabbedPropertySheetPage = tabbedPropertySheetPage;
	}

	public void setTableProvider(AbstractTableProvider provider) {
		tableProvider = provider;
	}
	
	/**
	 * Create a default ColumnTableProvider if none was set in setTableProvider();
	 * @param object
	 * @param feature
	 * @return
	 */
	public AbstractTableProvider getTableProvider(EObject object, EStructuralFeature feature) {
		if (tableProvider==null) {
			final EList<EObject> list = (EList<EObject>)object.eGet(feature);
			final EClass listItemClass = (EClass) feature.getEType();

			tableProvider = new AbstractTableProvider() {
				@Override
				public boolean canModify(EObject object, EStructuralFeature feature, EObject item) {
					return true;
				}
			};
			
			for (EAttribute a1 : listItemClass.getEAllAttributes()) {
				if ("anyAttribute".equals(a1.getName())) {
					List<EStructuralFeature> anyAttributes = new ArrayList<EStructuralFeature>();
					// are there any actual "anyAttribute" instances we can look at
					// to get the feature names and types from?
					// TODO: enhance the table to dynamically allow creation of new
					// columns which will be added to the "anyAttributes"
					for (EObject instance : list) {
						Object o = instance.eGet(a1);
						if (o instanceof BasicFeatureMap) {
							BasicFeatureMap map = (BasicFeatureMap)o;
							for (Entry entry : map) {
								EStructuralFeature f1 = entry.getEStructuralFeature();
								if (f1 instanceof EAttribute && !anyAttributes.contains(f1)) {
									tableProvider.add(new TableColumn(object,(EAttribute)f1));
									anyAttributes.add(f1);
								}
							}
						}
					}
				}
				else if (FeatureMap.Entry.class.equals(a1.getEType().getInstanceClass())) {
					// TODO: how do we handle these?
					if (a1 instanceof EAttribute)
						tableProvider.add(new TableColumn(object,a1));
					else
						System.out.println("FeatureMapEntry: "+listItemClass.getName()+"."+a1.getName());
				}
				else {
					tableProvider.add(new TableColumn(object,a1));
				}
			}
		}
		return tableProvider;
	}
	
	/**
	 * Override this to create your own Details section. This composite will be displayed
	 * in a twistie section whenever the user selects an item from the table. The section
	 * is automatically hidden when the table is collapsed.
	 * 
	 * @param parent
	 * @return
	 */
	public Composite createDetailComposite(Composite parent) {
		return new DefaultPropertiesComposite(parent, SWT.NONE);
	}
	
	/**
	 * Override this if construction of new list items needs special handling. 
	 * @param object
	 * @param feature
	 * @return
	 */
	protected EObject addListItem(EObject object, EStructuralFeature feature) {
		EList<EObject> list = (EList<EObject>)object.eGet(feature);
		EClass listItemClass = (EClass) feature.getEType();
		if (!(list instanceof EObjectContainmentEList)) {
			// this is not a containment list so we can't add it
			// because we don't know where the new object belongs
			MessageDialog.openError(getShell(), "Internal Error",
					"Can not create a new " +
					listItemClass.getName() +
					" because the list is not a container. " +
					"The default addListItem() method must be implemented."
			);
			return null;
		}
		EObject newItem = MODEL_FACTORY.create(listItemClass);
		list.add(newItem);
		ModelUtil.addID(newItem);
		return newItem;
	}

	/**
	 * Override this if editing of new list items needs special handling. 
	 * @param object
	 * @param feature
	 * @return
	 */
	protected EObject editListItem(EObject object, EStructuralFeature feature) {
		return null;
	}	
	/**
	 * Override this if removal of list items needs special handling. 
	 * @param object
	 * @param feature
	 * @param item
	 * @return
	 */
	protected boolean removeListItem(EObject object, EStructuralFeature feature, Object item) {
		EList<EObject> list = (EList<EObject>)object.eGet(feature);
		list.remove(item);
		return true;
	}

	protected void bindList(final EObject object, final EStructuralFeature feature, ItemProviderAdapter itemProviderAdapter) {
		if (!(object.eGet(feature) instanceof EList<?>)) {
			return;
		}
		Class<?> clazz = feature.getEType().getInstanceClass();
		if (!EObject.class.isAssignableFrom(clazz)) {
			return;
		}
		TransactionChangeRecorder cr = null;
		TransactionalEditingDomain dom = null;
		for (Adapter ad : object.eAdapters()) {
			if (ad instanceof TransactionChangeRecorder) {
				cr = (TransactionChangeRecorder)ad;
				dom = cr.getEditingDomain();
			}
		}

		if (bpmn2Editor==null)
			bpmn2Editor = BPMN2Editor.getEditor(object);
		if (bpmn2Editor==null)
			return;
		
		final TransactionalEditingDomain editingDomain = bpmn2Editor.getEditingDomain();
		final EList<EObject> list = (EList<EObject>)object.eGet(feature);
		final EClass listItemClass = (EClass) feature.getEType();
		
		////////////////////////////////////////////////////////////
		// Collect columns to be displayed and build column provider
		////////////////////////////////////////////////////////////
		if (getTableProvider(object, feature).getColumns().size()==0) {
			return;
		}

		////////////////////////////////////////////////////////////
		// SashForm contains the table section and a possible
		// details section
		////////////////////////////////////////////////////////////
		if ((style & HIDE_TITLE)==0 || (style & SHOW_DETAILS)!=0) {
			// display title in the table section and/or show a details section
			// SHOW_DETAILS forces drawing of a section title
			sashForm = toolkit.createSashForm(this, SWT.NONE);
			sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			
			tableSection = toolkit.createSection(sashForm, ModelUtil.toDisplayName(feature.getName()),
					ExpandableComposite.TWISTIE |
					ExpandableComposite.COMPACT |
					ExpandableComposite.TITLE_BAR);

			tableComposite = toolkit.createComposite(tableSection, SWT.NONE);
			tableSection.setClient(tableComposite);
			tableComposite.setLayout(new GridLayout(3, false));
			createTableAndButtons(tableComposite,style);
			
			detailSection = toolkit.createSection(sashForm, ModelUtil.toDisplayName(listItemClass.getName()) + " Details");
			detailComposite = createDetailComposite(detailSection);
			detailSection.setClient(detailComposite);
			toolkit.track(detailComposite);
			
			detailSection.setVisible(false);

			tableSection.addExpansionListener(new IExpansionListener() {

				@Override
				public void expansionStateChanging(ExpansionEvent e) {
					if (!e.getState()) {
						detailSection.setVisible(false);
					}
				}

				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					redrawPage();
				}
				
			});
			
			detailSection.addExpansionListener(new IExpansionListener() {

				@Override
				public void expansionStateChanging(ExpansionEvent e) {
					if (!e.getState()) {
						detailSection.setVisible(false);
					}
				}

				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					redrawPage();
				}
				
			});
			
			sashForm.setWeights(new int[] { 1, 1 });
		}
		else {
			createTableAndButtons(this,style);
		}
		
		////////////////////////////////////////////////////////////
		// Create table viewer and cell editors
		////////////////////////////////////////////////////////////
		tableViewer = new TableViewer(table);
		tableProvider.createTableLayout(table);
		tableProvider.setTableViewer(tableViewer);
		
		tableViewer.setLabelProvider(tableProvider);
		tableViewer.setCellModifier(tableProvider);
		tableViewer.setContentProvider(new ContentProvider(object, list));
		tableViewer.setColumnProperties(tableProvider.getColumnProperties());
		tableViewer.setCellEditors(tableProvider.createCellEditors(table));

		////////////////////////////////////////////////////////////
		// add a resource set listener to update the treeviewer when
		// when something interesting happens
		////////////////////////////////////////////////////////////
		final ResourceSetListenerImpl domainListener = new ResourceSetListenerImpl() {
			@Override
			public void resourceSetChanged(ResourceSetChangeEvent event) {
				List<Notification> notifications = event.getNotifications();
				try {
					for (Notification notification : notifications) {
						tableViewer.setInput(list);
					}
				}
				catch (Exception e) {
					// silently ignore :-o
				}
			}
		};
		editingDomain.addResourceSetListener(domainListener);
		table.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				editingDomain.removeResourceSetListener(domainListener);
			}
		});

		////////////////////////////////////////////////////////////
		// Create handlers
		////////////////////////////////////////////////////////////
		tableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enable = !event.getSelection().isEmpty();
				if (detailSection!=null) {
					detailSection.setVisible(enable);
					if (enable) {
						IStructuredSelection sel = (IStructuredSelection) event.getSelection();
						if (sel.getFirstElement() instanceof EObject)
							((DefaultPropertiesComposite)detailComposite).setEObject(bpmn2Editor,(EObject)sel.getFirstElement());
					}
					sashForm.layout(true);
					redrawPage();
				}
				if (removeButton!=null)
					removeButton.setEnabled(enable);
				if (editButton!=null)
					editButton.setEnabled(enable);
				if (upButton!=null && downButton!=null) {
					int i = table.getSelectionIndex();
					if (i>0)
						upButton.setEnabled(enable);
					else
						upButton.setEnabled(false);
					if (i<table.getItemCount()-1)
						downButton.setEnabled(enable);
					else
						downButton.setEnabled(false);
				}
			}
		});
		
		if (addButton!=null) {
			addButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							EObject newItem = addListItem(object,feature);
							if (newItem!=null) {
								tableViewer.setInput(list);
								tableViewer.setSelection(new StructuredSelection(newItem));
							}
						}
					});
				}
			});
		}

		if (removeButton!=null) {
			removeButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							if (removeListItem(object,feature,list.get(i))) {
								tableViewer.setInput(list);
								if (i>=list.size())
									i = list.size() - 1;
								if (i>=0)
									tableViewer.setSelection(new StructuredSelection(list.get(i)));
							}
						}
					});
				}
			});
		}

		if (upButton!=null && downButton!=null) {
			upButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							list.move(i-1, i);
							tableViewer.setInput(list);
							tableViewer.setSelection(new StructuredSelection(list.get(i-1)));
						}
					});
				}
			});
			
			downButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							int i = table.getSelectionIndex();
							list.move(i+1, i);
							tableViewer.setInput(list);
							tableViewer.setSelection(new StructuredSelection(list.get(i+1)));
						}
					});
				}
			});
		}

		if (editButton!=null) {
			editButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
						@Override
						protected void doExecute() {
							EObject newItem = editListItem(object,feature);
							if (newItem!=null) {
								tableViewer.setInput(list);
								tableViewer.setSelection(new StructuredSelection(newItem));
							}
						}
					});
				}
			});
		}
		
		tableViewer.setInput(list);
		
		// a TableCursor allows navigation of the table with keys
		TableCursor.create(table, tableViewer);
		redrawPage();
	}
	
	protected void redrawPage() {
		if (tabbedPropertySheetPage!=null) {
			Composite composite = (Composite)tabbedPropertySheetPage.getControl();
			composite.layout(true);
			tabbedPropertySheetPage.resizeScrolledComposite();
		}
	}

	private void createTableAndButtons(Composite parent, int style) {

		GridData gridData;
		
		////////////////////////////////////////////////////////////
		// Create a composite to hold the buttons and table
		////////////////////////////////////////////////////////////
		tableAndButtonsComposite = toolkit.createComposite(parent, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		tableAndButtonsComposite.setLayoutData(gridData);
		tableAndButtonsComposite.setLayout(new GridLayout(2, false));
		
		////////////////////////////////////////////////////////////
		// Create button section for add/remove/up/down buttons
		////////////////////////////////////////////////////////////
		buttonsComposite = toolkit.createComposite(tableAndButtonsComposite);
		buttonsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		buttonsComposite.setLayout(new FillLayout(SWT.VERTICAL));

		////////////////////////////////////////////////////////////
		// Create table
		// allow table to fill entire width if there are no buttons
		////////////////////////////////////////////////////////////
		int span = 2;
		if ((style & CUSTOM_BUTTONS_MASK)!=0) {
			span = 1;
		}
		else {
			buttonsComposite.setVisible(false);
		}
		table = toolkit.createTable(tableAndButtonsComposite, SWT.FULL_SELECTION | SWT.V_SCROLL);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, span, 1);
		gridData.widthHint = 100;
		gridData.heightHint = 100;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		////////////////////////////////////////////////////////////
		// Create buttons for add/remove/up/down
		////////////////////////////////////////////////////////////
		if ((style & ADD_BUTTON)!=0) {
			addButton = toolkit.createPushButton(buttonsComposite, "Add");
		}

		if ((style & REMOVE_BUTTON)!=0) {
			removeButton = toolkit.createPushButton(buttonsComposite, "Remove");
			removeButton.setEnabled(false);
		}
		
		if ((style & MOVE_BUTTONS)!=0) {
			upButton = toolkit.createPushButton(buttonsComposite, "Up");
			upButton.setEnabled(false);
	
			downButton = toolkit.createPushButton(buttonsComposite, "Down");
			downButton.setEnabled(false);
		}
		
		if ((style & EDIT_BUTTON)!=0) {
			editButton = toolkit.createPushButton(buttonsComposite, "Edit...");
			editButton.setEnabled(false);
		}

		
	}
	
	public class ContentProvider implements IStructuredContentProvider {
		private EObject parent;
		private EList<EObject> list;
		
		public ContentProvider(EObject p, EList<EObject> l) {
			parent = p;
			list = l;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return list.toArray();
		}
	}
	
	public class TableColumn extends ColumnTableProvider.Column implements ILabelProvider, ICellModifier {
		private TableViewer tableViewer;
		private EStructuralFeature feature;
		private EObject object;
		
		public TableColumn(EObject o, EAttribute a) {
			object = o;
			feature = a;
		}
		
		public void setTableViewer(TableViewer t) {
			tableViewer = t;
		}
		
		@Override
		public String getHeaderText() {
			return ModelUtil.toDisplayName(feature.getName());
		}

		@Override
		public String getProperty() {
			return feature.getName(); //$NON-NLS-1$
		}

		@Override
		public int getInitialWeight() {
			return 10;
		}

		public String getText(Object element) {
			Object value = ((EObject)element).eGet(feature);
			return value==null ? "" : value.toString();
		}
		
		public CellEditor createCellEditor (Composite parent) {			
			EClassifier ec = feature.getEType();
			if (ec instanceof EDataType) {
				return new EDataTypeCellEditor((EDataType)ec, parent);
			}
			return null;
		}
		
		public boolean canModify(Object element, String property) {
			return tableProvider.canModify(object, feature, (EObject)element);
		}

		public void modify(Object element, String property, Object value) {
			final EObject target = (EObject)element;
			final Object newValue = value;
			final Object oldValue = target.eGet(feature); 
			if (oldValue==null || !oldValue.equals(value)) {
				TransactionalEditingDomain editingDomain = bpmn2Editor.getEditingDomain();
				editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
					@Override
					protected void doExecute() {
						target.eSet(feature, newValue);
					}
				});
				if (bpmn2Editor.getDiagnostics()!=null) {
					// revert the change and display error status message.
					bpmn2Editor.showErrorMessage(bpmn2Editor.getDiagnostics().getMessage());
				}
				else
					bpmn2Editor.showErrorMessage(null);
				tableViewer.refresh();
			}
		}

		@Override
		public Object getValue(Object element, String property) {
			return getText(element);
		}
	}

	public abstract class AbstractTableProvider extends ColumnTableProvider {
		
		/**
		 * Implement this to select which columns are editable
		 * @param object - the list object
		 * @param feature - the feature of the item contained in the list
		 * @param item - the selected item in the list
		 * @return true to allow editing
		 */
		public abstract boolean canModify(EObject object, EStructuralFeature feature, EObject item);
	}
}

package org.camunda.bpm.modeler.ui.property.tabs.builder.table;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.modeler.ui.property.tabs.binding.change.EObjectChangeSupport;
import org.camunda.bpm.modeler.ui.property.tabs.tables.EObjectAttributeTableColumnDescriptor;
import org.camunda.bpm.modeler.ui.property.tabs.tables.EObjectAttributeTableColumnDescriptor.EditingSupportProvider;
import org.camunda.bpm.modeler.ui.property.tabs.tables.EditableTableDescriptor;
import org.camunda.bpm.modeler.ui.property.tabs.tables.EditableTableDescriptor.CellEditingStrategy;
import org.camunda.bpm.modeler.ui.property.tabs.tables.EditableTableDescriptor.ElementFactory;
import org.camunda.bpm.modeler.ui.property.tabs.tables.TableColumnDescriptor;
import org.camunda.bpm.modeler.ui.property.tabs.util.Events;
import org.camunda.bpm.modeler.ui.property.tabs.util.Events.DeleteRow;
import org.camunda.bpm.modeler.ui.property.tabs.util.Events.RowDeleted;
import org.camunda.bpm.modeler.ui.property.tabs.util.PropertyUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.NotificationFilter;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

public class EObjectTableBuilder<T extends EObject> {

	protected GFPropertySection section;
	protected Composite parent;

	private ElementFactory<T> elementFactory;

	private AddedRowHandler<T> addHandler;

	private DeleteRowHandler<T> deleteRowHandler;
	private SelectedRowHandler<T> selectHandler;

	protected EStructuralFeature[] columnFeatures;
	private String[] columnLabels;
	private NotificationFilter changeFilter;

	private ContentProvider<T> contentProvider;
	private EditingSupportProvider editingSupportProvider;

	protected EObject model;

	protected Class<T> genericTypeCls;

	public EObjectTableBuilder(final GFPropertySection section,
			final Composite parent, final Class<T> genericTypeCls) {
		this.section = section;
		this.parent = parent;

		this.genericTypeCls = genericTypeCls;
	}

	public EObjectTableBuilder<T> deleteRowHandler(
			final DeleteRowHandler<T> deleteRowHandler) {
		this.deleteRowHandler = deleteRowHandler;

		return this;
	}

	public EObjectTableBuilder<T> selectedRowHandler(
			final SelectedRowHandler<T> selectHandler) {
		this.selectHandler = selectHandler;

		return this;
	}

	public EObjectTableBuilder<T> addedRowHandler(
			final AddedRowHandler<T> addHandler) {
		this.addHandler = addHandler;

		return this;
	}

	public EObjectTableBuilder<T> elementFactory(
			final ElementFactory<T> elementFactory) {
		this.elementFactory = elementFactory;
		return this;
	}

	public EObjectTableBuilder<T> columnFeatures(
			final EStructuralFeature... columnFeatures) {
		this.columnFeatures = columnFeatures;

		return this;
	}

	public EObjectTableBuilder<T> columnLabels(final String... columnLabels) {
		this.columnLabels = columnLabels;

		return this;
	}

	public EObjectTableBuilder<T> model(final EObject model) {
		this.model = model;

		return this;
	}

	public EObjectTableBuilder<T> editingSupportProvider(
			final EditingSupportProvider editingSupportProvider) {
		this.editingSupportProvider = editingSupportProvider;

		return this;
	}

	protected EditableTableDescriptor<T> createTableDescriptor() {
		EditableTableDescriptor<T> tableDescriptor = new EditableTableDescriptor<T>();
		if (elementFactory != null) {
			tableDescriptor.setElementFactory(elementFactory);
		} else {
			tableDescriptor.setCellEditingStrategy(CellEditingStrategy.NO_EDIT);
			tableDescriptor.setElementFactory(elementFactory);
		}
		return tableDescriptor;
	}

	protected EObjectAttributeTableColumnDescriptor<T> createAttributeTableColumnDescriptor(
			final EStructuralFeature columnFeature, final String columnLabel,
			final int weight) {
		EObjectAttributeTableColumnDescriptor<T> columnDescriptor = new EObjectAttributeTableColumnDescriptor<T>(
				columnFeature, columnLabel, 30);
		columnDescriptor.setEditingSupportProvider(editingSupportProvider);

		return columnDescriptor;
	}

	public TableViewer build(boolean editable) {

		if (model == null) {
			throw new IllegalArgumentException("Model is null");
		}

		if (columnFeatures == null) {
			throw new IllegalArgumentException("Column features are null");
		}

		if (columnLabels == null) {
			throw new IllegalArgumentException("ColumnLabels are null");
		}

		// table descriptor
		EditableTableDescriptor<T> tableDescriptor = createTableDescriptor();

		List<TableColumnDescriptor> columns = new ArrayList<TableColumnDescriptor>();

		for (int i = 0; i < columnFeatures.length; i++) {
			EStructuralFeature columnFeature = columnFeatures[i];
			String columnLabel = columnLabels[i];

			EObjectAttributeTableColumnDescriptor<T> descriptor = createAttributeTableColumnDescriptor(
					columnFeature, columnLabel, 30);

			columns.add(descriptor);
		}

		tableDescriptor.setColumns(columns);

		// create composite
		Composite tableComposite = createTableComposite();

		// create viewer
		TableViewer tableViewer = tableDescriptor.createTableViewer(
				tableComposite, editable);

		// configure viewer
		configureViewer(tableViewer);

		// establish the binding between model and view
		establishModelViewBinding(model, tableViewer);

		// update viewer contents
		updateViewerContents(tableViewer);

		return tableViewer;
	}

	protected void establishModelViewBinding(final EObject model,
			final TableViewer tableViewer) {

		Table table = tableViewer.getTable();

		EObjectChangeSupport changeSupport = new EObjectChangeSupport(model,
				table);
		changeSupport.setFilter(changeFilter);
		changeSupport.register();

		if (deleteRowHandler != null) {
			table.addListener(Events.DELETE_ROW, new Listener() {
				@Override
				public void handleEvent(final Event e) {
					Events.DeleteRow<T> event = (DeleteRow<T>) e;

					T removedElement = event.getRemovedElement();

					if (deleteRowHandler != null) {
						boolean canDelete = deleteRowHandler
								.canDelete(removedElement);
						if (!canDelete) {
							event.setRejected();
						}
					}
				}
			});

			table.addListener(Events.ROW_DELETED, new Listener() {

				@Override
				public void handleEvent(final Event e) {
					Events.RowDeleted<T> event = (RowDeleted<T>) e;

					T removedElement = event.getRemovedElement();

					if (deleteRowHandler != null) {
						deleteRowHandler.rowDeleted(removedElement);
					}
				}
			});

		}
		tableViewer
				.addPostSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						T element = (T) selection.getFirstElement();

						if (selectHandler != null) {
							selectHandler.rowSelected(element);
						}
					}
				});

		table.addListener(Events.MODEL_CHANGED, new Listener() {

			@Override
			public void handleEvent(final Event e) {
				updateViewerContents(tableViewer);
			}
		});
	}

	protected void updateViewerContents(final TableViewer viewer) {
		List<T> contents = contentProvider.getContents();

		viewer.setInput(contents);
	}

	/**
	 * Can be overridden by subclasses to perform a post construct configuration
	 * of the selected viewer.
	 * 
	 * @param tableViewer
	 */
	protected void configureViewer(final TableViewer tableViewer) {

	}

	public EObjectTableBuilder<T> changeFilter(
			final NotificationFilter changeFilter) {
		this.changeFilter = changeFilter;

		return this;
	}

	public EObjectTableBuilder<T> contentProvider(
			final ContentProvider<T> contentProvider) {
		this.contentProvider = contentProvider;
		return this;
	}

	protected Composite createTableComposite() {

		Composite tableComposite = new Composite(parent, SWT.NONE);
		FormData tableCompositeFormData = PropertyUtil.getStandardLayout();
		tableCompositeFormData.height = 100;

		tableComposite.setLayoutData(tableCompositeFormData);

		// PropertyUtil.attachNote(tableComposite, HelpText.TABLE_HELP);

		return tableComposite;
	}

	/**
	 * Deletion handler
	 * 
	 * @author nico.rehwaldt
	 * 
	 * @param <T>
	 */
	public static interface DeleteRowHandler<T> {

		public void rowDeleted(T element);

		public boolean canDelete(T element);
	}

	public static abstract class AbstractDeleteRowHandler<T> implements
			DeleteRowHandler<T> {

		@Override
		public boolean canDelete(final T element) {
			return true;
		}
	}

	/**
	 * Selection handler
	 * 
	 * @author nico.rehwaldt
	 * 
	 * @param <T>
	 */
	public static interface SelectedRowHandler<T> {

		public void rowSelected(T element);
	}

	/**
	 * Addition handler
	 * 
	 * @author nico.rehwaldt
	 * 
	 * @param <T>
	 */
	public static interface AddedRowHandler<T> {

		public void rowAdded(T element);
	}

	/**
	 * Edit row handler
	 * 
	 * @author nico.rehwaldt
	 * 
	 * @param <T>
	 */
	public static interface EditRowHandler<T> {

		public void rowEdit(T element);

		public boolean canEdit(T element);
	}

	public static interface ContentProvider<T> {

		/**
		 * Returns the table contents
		 * 
		 * @return
		 */
		public List<T> getContents();
	}
}

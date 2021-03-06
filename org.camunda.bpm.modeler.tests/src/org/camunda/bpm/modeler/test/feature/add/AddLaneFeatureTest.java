package org.camunda.bpm.modeler.test.feature.add;

import static org.camunda.bpm.modeler.test.util.assertions.Bpmn2ModelAssertions.assertThat;
import static org.camunda.bpm.modeler.test.util.operations.AddLaneOperation.addLane;
import static org.fest.assertions.api.Assertions.assertThat;

import org.camunda.bpm.modeler.core.layout.util.LayoutUtil;
import org.camunda.bpm.modeler.core.utils.BusinessObjectUtil;
import org.camunda.bpm.modeler.core.utils.LabelUtil;
import org.camunda.bpm.modeler.test.feature.AbstractFeatureTest;
import org.camunda.bpm.modeler.test.util.DiagramResource;
import org.camunda.bpm.modeler.test.util.Util;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.graphiti.datatypes.IRectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.junit.Test;

public class AddLaneFeatureTest extends AbstractFeatureTest {
	
	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToDiagram.bpmn")
	public void testAddOnDiagramNotAllowed() throws Exception {

		// given empty diagram
		
		// when
		// lane is added to it
		Object lane = addLane(diagramTypeProvider)
			.toContainer(diagram)
			.execute();
		
		// then
		// operation should be rejected
		assertThat(diagram)
			.hasNoChildren();
	}

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantChangesContainer() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "StartEvent_1");
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		ContainerShape newChildContainer = childShape.getContainer();
		
		// then
		// lane should be a container shape between participant and child shape
		assertThat(containerShape).hasChild(newChildContainer);
		
		Lane lane = BusinessObjectUtil.getFirstElementOfType(newChildContainer, Lane.class);
		assertThat(lane).isNotNull();
		
		// and lane should have the event as a new flow node ref
		assertThat(lane.getFlowNodeRefs()).contains((StartEvent) Util.findBusinessObjectById(containerShape, "StartEvent_1"));
	}
	
	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantRetainsContainedEventPosition() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "StartEvent_1");
		
		IRectangle preMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// then
		// contained element pos should be retained
		assertThat(postMoveShapePosition).isEqualTo(preMoveShapePosition);
	}

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantRetainsContainedTaskPosition() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "Task_1");
		
		IRectangle preMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// then
		// contained element pos should be retained
		assertThat(postMoveShapePosition).isEqualTo(preMoveShapePosition);
	}

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantRetainsContainedBoundaryEventPosition() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "BoundaryEvent_1");
		
		IRectangle preMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// then
		// contained element pos should be retained
		assertThat(postMoveShapePosition).isEqualTo(preMoveShapePosition);
	}

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantRetainsContainedBoundaryEventLabelPosition() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "BoundaryEvent_1");
		
		ContainerShape childLabelShape = LabelUtil.getLabelShape(childShape, diagram);
		
		IRectangle preMoveShapePosition = LayoutUtil.getAbsoluteBounds(childLabelShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveShapePosition = LayoutUtil.getAbsoluteBounds(childLabelShape);
		
		// then
		// contained element pos should be retained
		assertThat(postMoveShapePosition).isEqualTo(preMoveShapePosition);
	}
	
	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToNonEmptyParticipant.bpmn")
	public void testAddOnToParticipantRetainsContainedLabelPositions() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "_Participant_5");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "StartEvent_1");
		
		ContainerShape labelShape = LabelUtil.getLabelShape(childShape, diagram);
		
		IRectangle preMoveLabelShapePosition = LayoutUtil.getAbsoluteBounds(labelShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveLabelShapePosition = LayoutUtil.getAbsoluteBounds(labelShape);
		
		// then
		// label position of contained element pos should be retained
		assertThat(postMoveLabelShapePosition).isEqualTo(preMoveLabelShapePosition);
	}
	

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToLane.bpmn")
	public void testAddOnToLaneChangesContainer() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "Lane_1");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "StartEvent_1");
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		ContainerShape newChildContainer = childShape.getContainer();
		
		// then
		// lane should be a container shape between participant and child shape
		assertThat(containerShape).hasChild(newChildContainer);
		
		Lane lane = BusinessObjectUtil.getFirstElementOfType(newChildContainer, Lane.class);
		assertThat(lane).isNotNull();
		
		// and new lane should have the event as a new flow node ref
		assertThat(lane.getFlowNodeRefs()).contains((StartEvent) Util.findBusinessObjectById(containerShape, "StartEvent_1"));
	}

	@Test
	@DiagramResource("org/camunda/bpm/modeler/test/feature/add/AddFeatureTestBase.testAddToLane.bpmn")
	public void testAddOnToLaneRetainsContainedEventPosition() throws Exception {

		// given participant
		ContainerShape containerShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "Lane_1");
		ContainerShape childShape = (ContainerShape) Util.findShapeByBusinessObjectId(diagram, "StartEvent_1");
		
		IRectangle preMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// when
		// lane is added to it
		addLane(diagramTypeProvider)
			.toContainer(containerShape)
			.execute();
		
		IRectangle postMoveShapePosition = LayoutUtil.getAbsoluteBounds(childShape);
		
		// then
		// contained element pos should be retained
		assertThat(postMoveShapePosition).isEqualTo(preMoveShapePosition);
	}
}

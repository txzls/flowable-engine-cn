/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.validation.validator.impl;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class BoundaryEventValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<BoundaryEvent> boundaryEvents = process.findFlowElementsOfType(BoundaryEvent.class);

        // Only one boundary event of type 'cancel' can be attached to the same
        // element, so we store the count temporarily here
        HashMap<String, Integer> cancelBoundaryEventsCounts = new HashMap<>();

        // Only one boundary event of type 'compensate' can be attached to the
        // same element, so we store the count temporarily here
        HashMap<String, Integer> compensateBoundaryEventsCounts = new HashMap<>();

        for (int i = 0; i < boundaryEvents.size(); i++) {

            BoundaryEvent boundaryEvent = boundaryEvents.get(i);

            if (boundaryEvent.getEventDefinitions() != null && !boundaryEvent.getEventDefinitions().isEmpty()) {

                EventDefinition eventDefinition = boundaryEvent.getEventDefinitions().get(0);
                if (!(eventDefinition instanceof TimerEventDefinition) && 
                        !(eventDefinition instanceof ErrorEventDefinition) && 
                        !(eventDefinition instanceof SignalEventDefinition) && 
                        !(eventDefinition instanceof CancelEventDefinition) && 
                        !(eventDefinition instanceof MessageEventDefinition) && 
                        !(eventDefinition instanceof ConditionalEventDefinition) && 
                        !(eventDefinition instanceof CompensateEventDefinition) && 
                        !(eventDefinition instanceof EscalationEventDefinition) &&
                        !(eventDefinition instanceof VariableListenerEventDefinition)) {
                    
                    addError(errors, Problems.BOUNDARY_EVENT_INVALID_EVENT_DEFINITION, process, boundaryEvent, eventDefinition, "无效或不支持的事件定义");
                }

                if (eventDefinition instanceof CancelEventDefinition) {

                    FlowElement attachedToFlowElement = bpmnModel.getFlowElement(boundaryEvent.getAttachedToRefId());
                    if (!(attachedToFlowElement instanceof Transaction)) {
                        addError(errors, Problems.BOUNDARY_EVENT_CANCEL_ONLY_ON_TRANSACTION, process, boundaryEvent, "仅在事务子流程上支持带有cancelEventDefinition的边界事件");
                    } else {
                        if (!cancelBoundaryEventsCounts.containsKey(attachedToFlowElement.getId())) {
                            cancelBoundaryEventsCounts.put(attachedToFlowElement.getId(), Integer.valueOf(0));
                        }
                        cancelBoundaryEventsCounts.put(attachedToFlowElement.getId(), Integer.valueOf(cancelBoundaryEventsCounts.get(attachedToFlowElement.getId()) + 1));
                    }

                } else if (eventDefinition instanceof CompensateEventDefinition) {

                    if (!compensateBoundaryEventsCounts.containsKey(boundaryEvent.getAttachedToRefId())) {
                        compensateBoundaryEventsCounts.put(boundaryEvent.getAttachedToRefId(), Integer.valueOf(0));
                    }
                    compensateBoundaryEventsCounts.put(boundaryEvent.getAttachedToRefId(), compensateBoundaryEventsCounts.get(boundaryEvent.getAttachedToRefId()) + 1);

                } else if (eventDefinition instanceof MessageEventDefinition) {

                    // Check if other message boundary events with same message id
                    for (int j = 0; j < boundaryEvents.size(); j++) {
                        if (j != i) {
                            BoundaryEvent otherBoundaryEvent = boundaryEvents.get(j);
                            if (otherBoundaryEvent.getAttachedToRefId() != null && otherBoundaryEvent.getAttachedToRefId().equals(boundaryEvent.getAttachedToRefId())) {
                                if (otherBoundaryEvent.getEventDefinitions() != null && !otherBoundaryEvent.getEventDefinitions().isEmpty()) {
                                    EventDefinition otherEventDefinition = otherBoundaryEvent.getEventDefinitions().get(0);
                                    if (otherEventDefinition instanceof MessageEventDefinition) {
                                        MessageEventDefinition currentMessageEventDefinition = (MessageEventDefinition) eventDefinition;
                                        MessageEventDefinition otherMessageEventDefinition = (MessageEventDefinition) otherEventDefinition;
                                        if (otherMessageEventDefinition.getMessageRef() != null && otherMessageEventDefinition.getMessageRef().equals(currentMessageEventDefinition.getMessageRef())) {
                                            addError(errors, Problems.MESSAGE_EVENT_MULTIPLE_ON_BOUNDARY_SAME_MESSAGE_ID, process, boundaryEvent, "不支持具有相同消息id的多个消息事件");
                                        }
                                    }
                                }
                            }
                        }

                    }

                }

            } else {

                boolean isEventRegistryBoundaryEvent = false;
                List<ExtensionElement> eventTypeExtensionElements = boundaryEvent.getExtensionElements().get("eventType");
                if (eventTypeExtensionElements != null && !eventTypeExtensionElements.isEmpty()) {
                    String eventTypeValue = eventTypeExtensionElements.get(0).getElementText();
                    if (StringUtils.isNotEmpty(eventTypeValue)) {
                        isEventRegistryBoundaryEvent = true;
                    }
                }

                if (!isEventRegistryBoundaryEvent) {
                    addError(errors, Problems.BOUNDARY_EVENT_NO_EVENT_DEFINITION, process, boundaryEvent, "边界事件中缺少事件定义");
                }
            }
        }

        for (String elementId : cancelBoundaryEventsCounts.keySet()) {
            if (cancelBoundaryEventsCounts.get(elementId) > 1) {
                addError(errors, Problems.BOUNDARY_EVENT_MULTIPLE_CANCEL_ON_TRANSACTION, process, bpmnModel.getFlowElement(elementId),
                        "同一事务子流程不支持带有cancelEventDefinition的多个边界事件");
            }
        }

        for (String elementId : compensateBoundaryEventsCounts.keySet()) {
            if (compensateBoundaryEventsCounts.get(elementId) > 1) {
                addError(errors, Problems.COMPENSATE_EVENT_MULTIPLE_ON_BOUNDARY, process, bpmnModel.getFlowElement(elementId), "“compensate”类型的多个边界事件无效");
            }
        }

    }
}

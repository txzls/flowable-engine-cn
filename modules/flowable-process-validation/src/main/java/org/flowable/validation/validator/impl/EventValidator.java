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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * Validates rules that apply to all events (start event, boundary event, etc.)
 * 
 * @author jbarrez
 */
public class EventValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<Event> events = process.findFlowElementsOfType(Event.class);
        for (Event event : events) {
            if (event.getEventDefinitions() != null) {
                for (EventDefinition eventDefinition : event.getEventDefinitions()) {

                    if (eventDefinition instanceof MessageEventDefinition) {
                        handleMessageEventDefinition(bpmnModel, process, event, eventDefinition, errors);
                    } else if (eventDefinition instanceof SignalEventDefinition) {
                        handleSignalEventDefinition(bpmnModel, process, event, eventDefinition, errors);
                    } else if (eventDefinition instanceof TimerEventDefinition) {
                        handleTimerEventDefinition(process, event, eventDefinition, errors);
                    } else if (eventDefinition instanceof CompensateEventDefinition) {
                        handleCompensationEventDefinition(bpmnModel, process, event, eventDefinition, errors);
                    }

                }
            }
        }
    }

    protected void handleMessageEventDefinition(BpmnModel bpmnModel, Process process, Event event, EventDefinition eventDefinition, List<ValidationError> errors) {
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;

        if (StringUtils.isEmpty(messageEventDefinition.getMessageRef())) {

            if (StringUtils.isEmpty(messageEventDefinition.getMessageExpression())) {
                // message ref should be filled in
                addError(errors, Problems.MESSAGE_EVENT_MISSING_MESSAGE_REF, process, event, messageEventDefinition, "属性“messageRef“或“messageExpression“是必需的");
            }

        } else if (!bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
            // message ref should exist
            addError(errors, Problems.MESSAGE_EVENT_INVALID_MESSAGE_REF, process, event, messageEventDefinition, "无效的“messageRef“:在模型中找不到该id的消息");
        }
    }

    protected void handleSignalEventDefinition(BpmnModel bpmnModel, Process process, Event event, EventDefinition eventDefinition, List<ValidationError> errors) {
        SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;

        if (StringUtils.isEmpty(signalEventDefinition.getSignalRef())) {

            if (StringUtils.isEmpty(signalEventDefinition.getSignalExpression())) {
                addError(errors, Problems.SIGNAL_EVENT_MISSING_SIGNAL_REF, process, event, signalEventDefinition, "signalEventDefinition没有“signalRef“或“signalExpression“");
            }

        } else if (!bpmnModel.containsSignalId(signalEventDefinition.getSignalRef())) {
            addError(errors, Problems.SIGNAL_EVENT_INVALID_SIGNAL_REF, process, event, signalEventDefinition, "无效的“signalRef“:在模型中找不到该id的信号");
        }
    }

    protected void handleTimerEventDefinition(Process process, Event event, EventDefinition eventDefinition, List<ValidationError> errors) {
        TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
        if (StringUtils.isEmpty(timerEventDefinition.getTimeDate()) && StringUtils.isEmpty(timerEventDefinition.getTimeCycle()) && StringUtils.isEmpty(timerEventDefinition.getTimeDuration())) {
            // neither date, cycle or duration configured
            addError(errors, Problems.EVENT_TIMER_MISSING_CONFIGURATION, process, event, timerEventDefinition, "定时器需要配置(timeDate, timeCycle或timeDuration都需要)");
        }
    }

    protected void handleCompensationEventDefinition(BpmnModel bpmnModel, Process process, Event event, EventDefinition eventDefinition, List<ValidationError> errors) {
        CompensateEventDefinition compensateEventDefinition = (CompensateEventDefinition) eventDefinition;

        // Check activityRef
        if ((StringUtils.isNotEmpty(compensateEventDefinition.getActivityRef()) && process.getFlowElement(compensateEventDefinition.getActivityRef(), true) == null)) {
            addError(errors, Problems.COMPENSATE_EVENT_INVALID_ACTIVITY_REF, process, event, compensateEventDefinition, "“activityRef“的无效属性值:没有给定id的activity");
        }
    }

}

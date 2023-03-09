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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class UserTaskValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<UserTask> userTasks = process.findFlowElementsOfType(UserTask.class);
        for (UserTask userTask : userTasks) {
            if (userTask.getTaskListeners() != null) {
                for (FlowableListener listener : userTask.getTaskListeners()) {
                    if (listener.getEvent() == null) {
                        addError(errors, Problems.USER_TASK_LISTENER_MISSING_EVENT, process, userTask, listener,
                                "元素”event“在taskListener上是必需的");
                    }
                    if (ImplementationType.IMPLEMENTATION_TYPE_SCRIPT.equals(listener.getImplementationType())) {
                        if (listener.getScriptInfo() == null) {
                            addError(errors, Problems.USER_TASK_LISTENER_IMPLEMENTATION_MISSING, process, userTask, listener,
                                    "”script“类型的taskListener需要一个<script>子元素。");
                        }
                    } else if (listener.getImplementation() == null || listener.getImplementationType() == null) {
                        addError(errors, Problems.USER_TASK_LISTENER_IMPLEMENTATION_MISSING, process, userTask, listener,
                                "元素“class”或“expression”或“type=\"script\"”在taskListener上是必需的");
                    }
                }
            }
        }
    }

}

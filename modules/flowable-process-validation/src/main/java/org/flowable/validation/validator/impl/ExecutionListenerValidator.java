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
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class ExecutionListenerValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {

        validateListeners(process, null, process.getExecutionListeners(), errors);

        for (FlowElement flowElement : process.getFlowElements()) {
            validateListeners(process, flowElement, flowElement.getExecutionListeners(), errors);
        }
    }

    protected void validateListeners(Process process, FlowElement flowElement, List<FlowableListener> listeners, List<ValidationError> errors) {
        if (listeners != null) {
            for (FlowableListener listener : listeners) {

                if (ImplementationType.IMPLEMENTATION_TYPE_SCRIPT.equals(listener.getImplementationType())) {
                    if (listener.getScriptInfo() == null) {
                        addError(errors, Problems.EXECUTION_LISTENER_IMPLEMENTATION_MISSING, process, listener,
                                "”script“类型的executionListener需要<script>子元素。");
                    }
                } else if (listener.getImplementation() == null || listener.getImplementationType() == null) {
                    addError(errors, Problems.EXECUTION_LISTENER_IMPLEMENTATION_MISSING, process, flowElement, listener,
                            "在executionListener上必需有元素'class'或“expression”或type=\"script\"");
                }
            }
        }
    }
}

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
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class SequenceflowValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<SequenceFlow> sequenceFlows = process.findFlowElementsOfType(SequenceFlow.class);
        for (SequenceFlow sequenceFlow : sequenceFlows) {

            String sourceRef = sequenceFlow.getSourceRef();
            String targetRef = sequenceFlow.getTargetRef();

            if (StringUtils.isEmpty(sourceRef)) {
                addError(errors, Problems.SEQ_FLOW_INVALID_SRC, process, sequenceFlow, "序列流的源无效");
            }
            if (StringUtils.isEmpty(targetRef)) {
                addError(errors, Problems.SEQ_FLOW_INVALID_TARGET, process, sequenceFlow, "序列流的目标无效");
            }

            // Implicit check: sequence flow cannot cross (sub) process boundaries, hence we check the parent and not the process
            // (could be subprocess for example)
            FlowElement source = process.getFlowElement(sourceRef, true);
            FlowElement target = process.getFlowElement(targetRef, true);
            
            // Src and target validation
            if (source == null) {
                addError(errors, Problems.SEQ_FLOW_INVALID_SRC, process, sequenceFlow, "序列流的源无效");
            }
            
            if (target == null) {
                addError(errors, Problems.SEQ_FLOW_INVALID_TARGET, process, sequenceFlow, "序列流的目标无效");
            }

            if (source != null && target != null) {
                FlowElementsContainer sourceContainer = process.getFlowElementsContainer(source.getId());
                FlowElementsContainer targetContainer = process.getFlowElementsContainer(target.getId());
                
                if (sourceContainer == null) {
                    addError(errors, Problems.SEQ_FLOW_INVALID_SRC, process, sequenceFlow, "序列流的源无效");
                }
                
                if (targetContainer == null) {
                    addError(errors, Problems.SEQ_FLOW_INVALID_TARGET, process, sequenceFlow, "序列流的目标无效");
                }
                
                if (sourceContainer != null && targetContainer != null && !sourceContainer.equals(targetContainer)) {
                    addError(errors, Problems.SEQ_FLOW_INVALID_TARGET, process, sequenceFlow, "序列流的目标无效, 目标没有定义在与源相同的作用域中");
                }
            }
        }
    }

}

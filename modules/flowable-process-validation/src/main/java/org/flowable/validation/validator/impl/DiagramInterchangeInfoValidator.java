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
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ValidatorImpl;

/**
 * @author jbarrez
 */
public class DiagramInterchangeInfoValidator extends ValidatorImpl {

    @Override
    public void validate(BpmnModel bpmnModel, List<ValidationError> errors) {
        if (!bpmnModel.getLocationMap().isEmpty()) {

            // Location map
            for (String bpmnReference : bpmnModel.getLocationMap().keySet()) {
                if (bpmnModel.getFlowElement(bpmnReference) == null) {
                    // ACT-1625: don't warn when artifacts are referenced from
                    // DI
                    if (bpmnModel.getArtifact(bpmnReference) == null) {
                        // check if it's a Pool or Lane, then DI is ok
                        if (bpmnModel.getPool(bpmnReference) == null && bpmnModel.getLane(bpmnReference) == null) {
                            addWarning(errors, Problems.DI_INVALID_REFERENCE, null, bpmnModel.getFlowElement(bpmnReference), "图交换定义中的引用无效:无法找到 " + bpmnReference);
                        }
                    }
                } else if (!(bpmnModel.getFlowElement(bpmnReference) instanceof FlowNode)) {
                    addWarning(errors, Problems.DI_DOES_NOT_REFERENCE_FLOWNODE, null, bpmnModel.getFlowElement(bpmnReference), "图交换定义中的引用无效:无法找到 " + bpmnReference
                            + " 没有引用序列流");
                }
            }

        }

        if (!bpmnModel.getFlowLocationMap().isEmpty()) {
            // flowlocation map
            for (String bpmnReference : bpmnModel.getFlowLocationMap().keySet()) {
                if (bpmnModel.getFlowElement(bpmnReference) == null) {
                    // ACT-1625: don't warn when artifacts are referenced from
                    // DI
                    if (bpmnModel.getArtifact(bpmnReference) == null) {
                        addWarning(errors, Problems.DI_INVALID_REFERENCE, null, bpmnModel.getFlowElement(bpmnReference), "图交换定义中的引用无效:无法找到 " + bpmnReference);
                    }
                } else if (!(bpmnModel.getFlowElement(bpmnReference) instanceof SequenceFlow)) {
                    addWarning(errors, Problems.DI_DOES_NOT_REFERENCE_SEQ_FLOW, null, bpmnModel.getFlowElement(bpmnReference), "图交换定义中的引用无效:无法找到 " + bpmnReference
                            + " 没有引用序列流");
                }
            }
        }
    }
}

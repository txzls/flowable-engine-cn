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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class ExclusiveGatewayValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<ExclusiveGateway> gateways = process.findFlowElementsOfType(ExclusiveGateway.class);
        for (ExclusiveGateway gateway : gateways) {
            validateExclusiveGateway(process, gateway, errors);
        }
    }

    public void validateExclusiveGateway(Process process, ExclusiveGateway exclusiveGateway, List<ValidationError> errors) {
        if (exclusiveGateway.getOutgoingFlows().isEmpty()) {
            addError(errors, Problems.EXCLUSIVE_GATEWAY_NO_OUTGOING_SEQ_FLOW, process, exclusiveGateway, "独占网关没有传出序列流");
        } else if (exclusiveGateway.getOutgoingFlows().size() == 1) {
            SequenceFlow sequenceFlow = exclusiveGateway.getOutgoingFlows().get(0);
            if (StringUtils.isNotEmpty(sequenceFlow.getConditionExpression())) {
                addError(errors, Problems.EXCLUSIVE_GATEWAY_CONDITION_NOT_ALLOWED_ON_SINGLE_SEQ_FLOW, process, exclusiveGateway,
                        "只有一个出序流的独占网关不允许有条件。");
            }
        } else {
            String defaultSequenceFlow = exclusiveGateway.getDefaultFlow();

            List<SequenceFlow> flowsWithoutCondition = new ArrayList<>();
            for (SequenceFlow flow : exclusiveGateway.getOutgoingFlows()) {
                String condition = flow.getConditionExpression();
                boolean isDefaultFlow = flow.getId() != null && flow.getId().equals(defaultSequenceFlow);
                boolean hasCondition = StringUtils.isNotEmpty(condition);

                if (!hasCondition && !isDefaultFlow) {
                    flowsWithoutCondition.add(flow);
                }
                if (hasCondition && isDefaultFlow) {
                    addError(errors, Problems.EXCLUSIVE_GATEWAY_CONDITION_ON_DEFAULT_SEQ_FLOW, process, exclusiveGateway, "默认序列流不允许有条件");
                }
            }

            if (!flowsWithoutCondition.isEmpty()) {
                addWarning(errors, Problems.EXCLUSIVE_GATEWAY_SEQ_FLOW_WITHOUT_CONDITIONS, process, exclusiveGateway,
                        "独占网关有至少一个没有条件的(非默认的)传出序列流)");
            }

        }
    }

}

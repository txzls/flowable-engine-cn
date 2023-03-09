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
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataAssociation;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * A validator for stuff that is shared across all flow elements
 * 
 * @author jbarrez
 * @author Erik Winlof
 */
public class FlowElementValidator extends ProcessLevelValidator {

    protected static final int ID_MAX_LENGTH = 255;

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        for (FlowElement flowElement : process.getFlowElements()) {

            if (flowElement instanceof Activity) {
                Activity activity = (Activity) flowElement;
                handleConstraints(process, activity, errors);
                handleMultiInstanceLoopCharacteristics(process, activity, errors);
                handleDataAssociations(process, activity, errors);
            }

        }

    }

    protected void handleConstraints(Process process, Activity activity, List<ValidationError> errors) {
        if (activity.getId() != null && activity.getId().length() > ID_MAX_LENGTH) {
            addError(errors, Problems.FLOW_ELEMENT_ID_TOO_LONG, process, activity,
                    "流程元素的id不能超过 " + ID_MAX_LENGTH + " 个字符");
        }
    }

    protected void handleMultiInstanceLoopCharacteristics(Process process, Activity activity, List<ValidationError> errors) {
        MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = activity.getLoopCharacteristics();
        if (multiInstanceLoopCharacteristics != null) {

            if (StringUtils.isEmpty(multiInstanceLoopCharacteristics.getLoopCardinality())
                    && StringUtils.isEmpty(multiInstanceLoopCharacteristics.getInputDataItem()) && StringUtils.isEmpty(multiInstanceLoopCharacteristics.getCollectionString())) {

                addError(errors, Problems.MULTI_INSTANCE_MISSING_COLLECTION, process, activity, multiInstanceLoopCharacteristics,
                        "必须设置loopCardinality或loopDataInputRef/flowable:collection中的一个");
            }
            
            if (!StringUtils.isEmpty(multiInstanceLoopCharacteristics.getCollectionString())) {

            	if (multiInstanceLoopCharacteristics.getHandler() == null) {
            		// verify string parsing function attributes
            		addError(errors, Problems.MULTI_INSTANCE_MISSING_COLLECTION_FUNCTION_PARAMETERS, process, activity,
                            "flowable:collection元素字符串值需要函数参数flowable:delegateExpression或flowable:class。");
            	}
            }

        }
    }

    protected void handleDataAssociations(Process process, Activity activity, List<ValidationError> errors) {
        if (activity.getDataInputAssociations() != null) {
            for (DataAssociation dataAssociation : activity.getDataInputAssociations()) {
                if (StringUtils.isEmpty(dataAssociation.getTargetRef())) {
                    addError(errors, Problems.DATA_ASSOCIATION_MISSING_TARGETREF, process, activity, dataAssociation,
                            "在数据关联上需要“Targetref属性");
                }
            }
        }
        if (activity.getDataOutputAssociations() != null) {
            for (DataAssociation dataAssociation : activity.getDataOutputAssociations()) {
                if (StringUtils.isEmpty(dataAssociation.getTargetRef())) {
                    addError(errors, Problems.DATA_ASSOCIATION_MISSING_TARGETREF, process, activity,
                            "在数据关联上需要“Targetref属性");
                }
            }
        }
    }

}

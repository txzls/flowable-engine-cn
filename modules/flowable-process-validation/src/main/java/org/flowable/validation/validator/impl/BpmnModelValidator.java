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
import org.flowable.bpmn.model.Process;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Constraints;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ValidatorImpl;

/**
 * @author jbarrez
 * @author Erik Winlof
 */
public class BpmnModelValidator extends ValidatorImpl {

    @Override
    public void validate(BpmnModel bpmnModel, List<ValidationError> errors) {

        // If all process definitions of this bpmnModel are not executable, raise an error
        boolean isAtLeastOneExecutable = validateAtLeastOneExecutable(bpmnModel, errors);

        // If at least one process definition is executable, show a warning for each of the none-executables
        if (isAtLeastOneExecutable) {
            for (Process process : bpmnModel.getProcesses()) {
                if (!process.isExecutable()) {
                    addWarning(errors, Problems.PROCESS_DEFINITION_NOT_EXECUTABLE, process, process,
                            "流程定义不可执行。请确认这是有意为之");
                }
                handleProcessConstraints(bpmnModel, process, errors);
            }
        }
        handleBPMNModelConstraints(bpmnModel, errors);
    }

    protected void handleProcessConstraints(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        if (process.getId() != null && process.getId().length() > Constraints.PROCESS_DEFINITION_ID_MAX_LENGTH) {
            addError(errors, Problems.PROCESS_DEFINITION_ID_TOO_LONG, process,
                    "进程定义的id不能超过" + Constraints.PROCESS_DEFINITION_ID_MAX_LENGTH + "个字符");
        }
        if (process.getName() != null && process.getName().length() > Constraints.PROCESS_DEFINITION_NAME_MAX_LENGTH) {
            addError(errors, Problems.PROCESS_DEFINITION_NAME_TOO_LONG, process,
                    "进程定义的名称不能超过 " + Constraints.PROCESS_DEFINITION_NAME_MAX_LENGTH + "个字符");
        }
        if (process.getDocumentation() != null && process.getDocumentation().length() > Constraints.PROCESS_DEFINITION_DOCUMENTATION_MAX_LENGTH) {
            addError(errors, Problems.PROCESS_DEFINITION_DOCUMENTATION_TOO_LONG, process,
                    "流程定义的文档不能包含超过" + Constraints.PROCESS_DEFINITION_DOCUMENTATION_MAX_LENGTH + "个字符");
        }
    }

    protected void handleBPMNModelConstraints(BpmnModel bpmnModel, List<ValidationError> errors) {
        if (bpmnModel.getTargetNamespace() != null && bpmnModel.getTargetNamespace().length() > Constraints.BPMN_MODEL_TARGET_NAMESPACE_MAX_LENGTH) {
            addError(errors, Problems.BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG,
                    "bpmn模型的targetNamespace不能包含超过" + Constraints.BPMN_MODEL_TARGET_NAMESPACE_MAX_LENGTH + "个字符");
        }
    }

    /**
     * Returns 'true' if at least one process definition in the {@link BpmnModel} is executable.
     */
    protected boolean validateAtLeastOneExecutable(BpmnModel bpmnModel, List<ValidationError> errors) {
        int nrOfExecutableDefinitions = 0;
        for (Process process : bpmnModel.getProcesses()) {
            if (process.isExecutable()) {
                nrOfExecutableDefinitions++;
            }
        }

        if (nrOfExecutableDefinitions == 0) {
            addError(errors, Problems.ALL_PROCESS_DEFINITIONS_NOT_EXECUTABLE,
                    "不允许所有进程定义都设置为不可执行（进程上属性“isExecutable”）");
        }

        return nrOfExecutableDefinitions > 0;
    }

}

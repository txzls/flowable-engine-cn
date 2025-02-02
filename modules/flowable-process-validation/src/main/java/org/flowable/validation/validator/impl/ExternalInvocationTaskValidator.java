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
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.TaskWithFieldExtensions;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

public abstract class ExternalInvocationTaskValidator extends ProcessLevelValidator {

    protected void validateFieldDeclarationsForEmail(org.flowable.bpmn.model.Process process, TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, List<ValidationError> errors) {
        boolean recipientDefined = false;
        boolean textOrHtmlDefined = false;

        for (FieldExtension fieldExtension : fieldExtensions) {
            if ("to".equals(fieldExtension.getFieldName())) {
                recipientDefined = true;
            }
            if ("cc".equals(fieldExtension.getFieldName())) {
                recipientDefined = true;
            }
            if ("bcc".equals(fieldExtension.getFieldName())) {
                recipientDefined = true;
            }
            if ("html".equals(fieldExtension.getFieldName())) {
                textOrHtmlDefined = true;
            }
            if ("htmlVar".equals(fieldExtension.getFieldName())) {
                textOrHtmlDefined = true;
            }
            if ("text".equals(fieldExtension.getFieldName())) {
                textOrHtmlDefined = true;
            }
            if ("textVar".equals(fieldExtension.getFieldName())) {
                textOrHtmlDefined = true;
            }
        }

        if (!recipientDefined) {
            addError(errors, Problems.MAIL_TASK_NO_RECIPIENT, process, task, "邮件任务上没有定义收件人");
        }
        if (!textOrHtmlDefined) {
            addError(errors, Problems.MAIL_TASK_NO_CONTENT, process, task, "需要提供Text, html, textVar或htmlVar字段");
        }
    }

    protected void validateFieldDeclarationsForShell(org.flowable.bpmn.model.Process process, TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, List<ValidationError> errors) {
        boolean shellCommandDefined = false;

        for (FieldExtension fieldExtension : fieldExtensions) {
            String fieldName = fieldExtension.getFieldName();
            String fieldValue = fieldExtension.getStringValue();

            if ("command".equals(fieldName)) {
                shellCommandDefined = true;
            }

            if (("wait".equals(fieldName) || "redirectError".equals(fieldName) || "cleanEnv".equals(fieldName)) && !"true".equals(fieldValue.toLowerCase()) && !"false".equals(fieldValue.toLowerCase())) {
                addError(errors, Problems.SHELL_TASK_INVALID_PARAM, process, task, fieldExtension, "shell字段未定义参数值");
            }

        }

        if (!shellCommandDefined) {
            addError(errors, Problems.SHELL_TASK_NO_COMMAND, process, task, "在shell任务上没有定义shell命令");
        }
    }

    protected void validateFieldDeclarationsForDmn(org.flowable.bpmn.model.Process process, TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, List<ValidationError> errors) {
        boolean keyDefined = false;

        for (FieldExtension fieldExtension : fieldExtensions) {
            String fieldName = fieldExtension.getFieldName();
            String fieldValue = fieldExtension.getStringValue();

            if ("decisionTableReferenceKey".equals(fieldName) && fieldValue != null && fieldValue.length() > 0) {
                keyDefined = true;
                break;
            }
            if ("decisionServiceReferenceKey".equals(fieldName) && fieldValue != null && fieldValue.length() > 0) {
                keyDefined = true;
                break;
            }
        }

        if (!keyDefined) {
            addError(errors, Problems.DMN_TASK_NO_KEY, process, task, "dmn任务上没有定义决策表或决策服务引用键");
        }
    }

    protected void validateFieldDeclarationsForHttp(org.flowable.bpmn.model.Process process, TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, List<ValidationError> errors) {
        boolean requestMethodDefined = false;
        boolean requestUrlDefined = false;

        for (FieldExtension fieldExtension : fieldExtensions) {

            String fieldName = fieldExtension.getFieldName();
            String fieldValue = fieldExtension.getStringValue();
            String fieldExpression = fieldExtension.getExpression();

            if ("requestMethod".equals(fieldName) && ((fieldValue != null && fieldValue.length() > 0) || (fieldExpression != null && fieldExpression.length() > 0))) {
                requestMethodDefined = true;
            }

            if ("requestUrl".equals(fieldName) && ((fieldValue != null && fieldValue.length() > 0) || (fieldExpression != null && fieldExpression.length() > 0))) {
                requestUrlDefined = true;
            }
        }

        if (!requestMethodDefined) {
            addError(errors, Problems.HTTP_TASK_NO_REQUEST_METHOD, process, task, "http任务上没有定义请求方法");
        }

        if (!requestUrlDefined) {
            addError(errors, Problems.HTTP_TASK_NO_REQUEST_URL, process, task, "http任务上没有定义请求url");
        }

    }
    
    protected void validateFieldDeclarationsForCase(org.flowable.bpmn.model.Process process, CaseServiceTask caseServiceTask, List<ValidationError> errors) {
        if (StringUtils.isEmpty(caseServiceTask.getCaseDefinitionKey())) {
            addError(errors, Problems.CASE_TASK_NO_CASE_DEFINITION_KEY, process, caseServiceTask, "在案例任务上没有定义case定义键");
        }
    }
    
    protected void validateFieldDeclarationsForSendEventTask(org.flowable.bpmn.model.Process process, SendEventServiceTask sendEventServiceTask, List<ValidationError> errors) {
        if (StringUtils.isEmpty(sendEventServiceTask.getEventType())) {
            addError(errors, Problems.SEND_EVENT_TASK_NO_EVENT_TYPE, process, sendEventServiceTask, "发送事件任务上没有定义事件类型");
        }
        List<ExtensionElement> channelKeyExtensionElements = sendEventServiceTask.getExtensionElements().get("channelKey");
        if (channelKeyExtensionElements == null || channelKeyExtensionElements.isEmpty() || StringUtils.isEmpty(channelKeyExtensionElements.get(0).getElementText())) {
            List<ExtensionElement> systemChannelElements = sendEventServiceTask.getExtensionElements().get("systemChannel");
            if (systemChannelElements == null || systemChannelElements.isEmpty()) {
                addError(errors, Problems.SEND_EVENT_TASK_NO_OUTBOUND_CHANNEL, process, sendEventServiceTask, "发送事件任务上没有设置输出通道");
            }
        }
    }

    protected void validateExternalWorkerTask(org.flowable.bpmn.model.Process process, ExternalWorkerServiceTask externalWorkerServiceTask, List<ValidationError> errors) {
        if (StringUtils.isEmpty(externalWorkerServiceTask.getTopic())) {
            addError(errors, Problems.EXTERNAL_WORKER_TASK_NO_TOPIC, process, externalWorkerServiceTask, "在外部任务上没有定义主题");
        }
    }
}

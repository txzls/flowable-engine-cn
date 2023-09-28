package org.flowable.engine.impl.history.async.json.transformer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import java.util.Collections;
import java.util.List;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

public class HistoricActivityDeleteInfoHistoryJsonTransformer extends AbstractNeedsHistoricActivityHistoryJsonTransformer {
    public HistoricActivityDeleteInfoHistoryJsonTransformer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public List<String> getTypes() {
        return Collections.singletonList(HistoryJsonConstants.TYPE_ACTIVITY_DELETE_REASON);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        boolean isApplicable = super.isApplicable(historicalData, commandContext);

        if (!isApplicable
                && !historicalData.has(HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID)
                && historicalData.has(HistoryJsonConstants.EXECUTION_ID)
                && historicalData.has(HistoryJsonConstants.ACTIVITY_ID)
                && historicalData.has(HistoryJsonConstants.DELETE_REASON)) {
            // This is old data, before the runtime activities were used.
            // As such, the transformJson can be tried (the null check will make sure no wrong instance is changed)
            isApplicable = true;
        }

        return isApplicable;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        HistoricActivityInstanceEntity historicActivityInstance = findHistoricActivityInstance(commandContext, executionId, activityId);
        if (historicActivityInstance != null && historicActivityInstance.getDeleteReason() == null) {
            historicActivityInstance.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));
        }
    }
}

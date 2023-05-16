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

package org.flowable.cmmn.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.api.Task;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Case instance resource.
 *
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 * @author Jose Antonio Alvarez
 */
public class CaseInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormRepositoryService formRepositoryService;

    @Mock
    protected FormService formEngineFormService;

    /**
     * Test getting a list of case instance, using all possible filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstances() throws Exception {
        identityService.setAuthenticatedUserId("kermit");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("myCaseInstanceName")
                .businessKey("myBusinessKey")
                .businessStatus("myBusinessStatus")
                .start();
        identityService.setAuthenticatedUserId(null);
        String id = caseInstance.getId();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsPresentInDataResponse(url, id);

        // Case instance id
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?id=" + id;
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?id=anotherId";
        assertResultsPresentInDataResponse(url);
        
        // Case instance name
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?name=myCaseInstanceName";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?name=anotherName";
        assertResultsPresentInDataResponse(url);
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?nameLike=" + encode("%InstanceName");
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?nameLike=" + encode("another%");
        assertResultsPresentInDataResponse(url);
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?nameLikeIgnoreCase=" + encode("%INSTANCEName");
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?nameLikeIgnoreCase=" + encode("ANOTHER%");
        assertResultsPresentInDataResponse(url);

        // Case instance business key
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessKey=myBusinessKey";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessKey=anotherBusinessKey";
        assertResultsPresentInDataResponse(url);
        
        // Case instance business status
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessStatus=myBusinessStatus";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessStatus=anotherBusinessStatus";
        assertResultsPresentInDataResponse(url);
        
        // Case instance started by
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedBy=kermit";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedBy=fozzie";
        assertResultsPresentInDataResponse(url);
        
        Calendar todayCal = new GregorianCalendar();
        Calendar futureCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        Calendar historicCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        
        // Case instance started before
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedBefore=" + getISODateString(futureCal.getTime());
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedBefore=" + getISODateString(historicCal.getTime());;
        assertResultsPresentInDataResponse(url);
        
        // Case instance started after
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedAfter=" + getISODateString(historicCal.getTime());;
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?startedAfter=" + getISODateString(futureCal.getTime());;
        assertResultsPresentInDataResponse(url);
        
        // Case instance state
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?state=active";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?state=completed";
        assertResultsPresentInDataResponse(url);

        // Case definition key
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionKey=oneHumanTaskCase";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionKey=caseTwo";
        assertResultsPresentInDataResponse(url);

        // Case definition id
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId();
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionId=anotherId";
        assertResultsPresentInDataResponse(url);
        
        // Case definition name
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionName=" + encode(caseInstance.getCaseDefinitionName());
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionName=anotherName";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test getting a list of sorted case instance
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesSorted() throws Exception {
        Instant initialTime = Instant.now();
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime));
        String nowInstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("now").start().getId();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.plus(1, ChronoUnit.HOURS)));
        String nowPlus1InstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("nowPlus1").start().getId();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.minus(1, ChronoUnit.HOURS)));
        String nowMinus1InstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("nowMinus1").start().getId();

        List<String> sortedIds = new ArrayList<>();
        sortedIds.add(nowInstanceId);
        sortedIds.add(nowPlus1InstanceId);
        sortedIds.add(nowMinus1InstanceId);
        Collections.sort(sortedIds);

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsExactlyPresentInDataResponse(url, sortedIds.toArray(new String[0]));

        // Sort by start time
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?sort=startTime";
        assertResultsExactlyPresentInDataResponse(url, nowMinus1InstanceId, nowInstanceId, nowPlus1InstanceId);

        // Sort by start time desc
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?sort=startTime&order=desc";
        assertResultsExactlyPresentInDataResponse(url, nowPlus1InstanceId, nowInstanceId, nowMinus1InstanceId);
    }

    /**
     * Test getting a list of case instance, using all tenant filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesTenant() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        String id = caseInstance.getId();

        // Test without tenant id
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, id);

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

        try {
            caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").tenantId("myTenant")
                    .start();
            String idWithTenant = caseInstance.getId();

            // Test tenant id
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, idWithTenant);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantId=anotherTenant";
            assertResultsPresentInDataResponse(url);

            // Test tenant id like
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, idWithTenant);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%what");
            assertResultsPresentInDataResponse(url);

            // Test without tenant id
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, id);
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    /**
     * Test getting a list of case instance, using the variable selector
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesWithVariables() throws Exception {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").variable("someVar", "someValue").start();

        // Test without any parameters, no variables included by default
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(rootNode).isNotEmpty();
        assertThat(rootNode.get("data")).hasSize(1);
        JsonNode dataNode = rootNode.get("data").get(0);
        JsonNode variableNodes = dataNode.get("variables");
        assertThat(variableNodes).isEmpty();

        // Test excluding variables
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?includeCaseVariables=false";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(rootNode).isNotEmpty();
        assertThat(rootNode.get("data")).hasSize(1);
        dataNode = rootNode.get("data").get(0);
        variableNodes = dataNode.get("variables");
        assertThat(variableNodes).isEmpty();

        // Test including variables
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?includeCaseVariables=true";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(rootNode).isNotEmpty();
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "         variables: [ {"
                        + "                    name: 'someVar',"
                        + "                    value: 'someValue'"
                        + "                    } ]"
                        + "      } ]"
                        + "}");
    }

    /**
     * Test starting a case instance using caseDefinitionId, key caseDefinitionKey business-key.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCase() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using case definition key
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance).isNotNull();

        HistoricCaseInstance historicCaseInstance = historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicCaseInstance).isNotNull();
        assertThat(historicCaseInstance.getStartUserId()).isEqualTo("kermit");

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + caseInstance.getId() + "',"
                        + " businessKey: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId()) + "',"
                        + " caseDefinitionUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "'"
                        + "}");

        runtimeService.terminateCaseInstance(caseInstance.getId());

        // Start using case definition id
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult().getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance).isNotNull();

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + caseInstance.getId() + "',"
                        + " businessKey: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId()) + "',"
                        + " caseDefinitionUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "'"
                        + "}");

        runtimeService.terminateCaseInstance(caseInstance.getId());

        // Start using case definition id and business key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult().getId());
        requestNode.put("businessKey", "myBusinessKey");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " businessKey: 'myBusinessKey'"
                        + "}");

        // Start using case definition id and case name
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult().getId());
        requestNode.put("name", "My test name");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " name: 'My test name'"
                        + "}");
    }

    /**
     * Test starting a case instance passing in variables to set.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCaseWithVariables() throws Exception {
        ArrayNode variablesNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = variablesNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        ObjectNode integerVarNode = variablesNode.addObject();
        integerVarNode.put("name", "integerVariable");
        integerVarNode.put("value", 1234);
        integerVarNode.put("type", "integer");

        ObjectNode shortVarNode = variablesNode.addObject();
        shortVarNode.put("name", "shortVariable");
        shortVarNode.put("value", 123);
        shortVarNode.put("type", "short");

        ObjectNode longVarNode = variablesNode.addObject();
        longVarNode.put("name", "longVariable");
        longVarNode.put("value", 4567890L);
        longVarNode.put("type", "long");

        ObjectNode doubleVarNode = variablesNode.addObject();
        doubleVarNode.put("name", "doubleVariable");
        doubleVarNode.put("value", 123.456);
        doubleVarNode.put("type", "double");

        ObjectNode booleanVarNode = variablesNode.addObject();
        booleanVarNode.put("name", "booleanVariable");
        booleanVarNode.put("value", Boolean.TRUE);
        booleanVarNode.put("type", "boolean");

        // Date
        Calendar varCal = Calendar.getInstance();
        String isoString = getISODateString(varCal.getTime());
        ObjectNode dateVarNode = variablesNode.addObject();
        dateVarNode.put("name", "dateVariable");
        dateVarNode.put("value", isoString);
        dateVarNode.put("type", "date");

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using case definition key, passing in variables
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
        requestNode.set("variables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);

        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance).isNotNull();

        // Check if engine has correct variables set
        Map<String, Object> caseVariables = runtimeService.getVariables(caseInstance.getId());

        assertThat(caseVariables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", Boolean.TRUE),
                        entry("dateVariable", longDateFormat.parse(isoString))
                );
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCaseUsingKeyAndTenantId() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment tenantDeployment = null;

        try {
            // Deploy the same case, in another tenant
            tenantDeployment = repositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
                    .tenantId("tenant1").deploy();

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Start using case definition key, in tenant 1
            requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
            requestNode.put("tenantId", "tenant1");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            closeResponse(response);

            // Only one case should have been started
            CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getTenantId()).isEqualTo("tenant1");

        } finally {
            // Cleanup deployment in tenant
            if (tenantDeployment != null) {
                repositoryService.deleteDeployment(tenantDeployment.getId(), true);
            }
        }
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" }, tenantId = "tenant1")
    public void testStartCaseUsingDefinitionId() {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using case definition id
        requestNode.put("caseDefinitionId", caseDefinition.getId());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString(), StandardCharsets.UTF_8));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);

        // Only one case should have been started
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneHumanTaskCase");
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
        assertThat(caseInstance.getTenantId()).isEqualTo("tenant1");
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" }, tenantId = "tenant1")
    public void testStartCaseUsingDefinitionIdAndOverrideDefinitionTenantId() {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using case definition id
        requestNode.put("caseDefinitionId", caseDefinition.getId());
        requestNode.put("overrideDefinitionTenantId", "tenant2");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString(), StandardCharsets.UTF_8));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);

        // Only one case should have been started
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneHumanTaskCase");
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
        assertThat(caseInstance.getTenantId()).isEqualTo("tenant2");
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskCaseWithStartForm.cmmn" })
    public void testStartCaseWithForm() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();

        runUsingMocks(() -> {
            Map engineConfigurations = cmmnEngineConfiguration.getEngineConfigurations();
            engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);

            FormInfo formInfo = new FormInfo();
            formInfo.setId("formDefId");
            formInfo.setKey("formDefKey");
            formInfo.setName("Form Definition Name");

            when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
            when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId(), caseDefinition.getTenantId(),
                    cmmnEngineConfiguration.isFallbackToDefaultTenant()))
                    .thenReturn(formInfo);

            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_START_FORM, caseDefinition.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " id: 'formDefId',"
                            + " key: 'formDefKey',"
                            + " name: 'Form Definition Name',"
                            + " type: 'startForm',"
                            + " definitionKey: 'oneHumanTaskCase'"
                            + "}");

            ArrayNode formVariablesNode = objectMapper.createArrayNode();

            // String variable
            ObjectNode stringVarNode = formVariablesNode.addObject();
            stringVarNode.put("name", "user");
            stringVarNode.put("value", "simple string value");
            stringVarNode.put("type", "string");

            ObjectNode integerVarNode = formVariablesNode.addObject();
            integerVarNode.put("name", "number");
            integerVarNode.put("value", 1234);
            integerVarNode.put("type", "integer");

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Start using case definition key
            requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
            requestNode.set("startFormVariables", formVariablesNode);

            when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId()))
                    .thenReturn(formInfo);
            when(formEngineConfiguration.getFormService()).thenReturn(formEngineFormService);
            when(formEngineFormService.getVariablesFromFormSubmission(formInfo, Map.of("user", "simple string value", "number", 1234), null))
                    .thenReturn(Map.of("user", "simple string value return", "number", 1234L));

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            response = executeRequest(httpPost, HttpStatus.SC_CREATED);

            CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
            assertThat(caseInstance).isNotNull();

            assertThat(runtimeService.getVariable(caseInstance.getId(), "user")).isEqualTo("simple string value return");
            assertThat(runtimeService.getVariable(caseInstance.getId(), "number")).isEqualTo(1234L);
        });
    }

    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/CaseInstanceCollectionResourceTest.oneHumanTaskCaseTestCategory.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/CaseInstanceCollectionResourceTest.oneHumanTaskCaseExampleCategory.cmmn"
    })
    public void testGetCaseInstancesByCategory() throws Exception {
        CaseInstance exampleCase = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCaseExampleCategory")
                .businessKey("example")
                .start();
        CaseInstance testCase = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCaseTestCategory")
                .businessKey("test")
                .start();

        String exampleCaseId = exampleCase.getId();
        String testCaseId = testCase.getId();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsPresentInDataResponse(url, testCaseId, exampleCaseId);

        // Case Definition Category
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionCategory=Example";
        assertResultsPresentInDataResponse(url, exampleCaseId);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionCategory=Unknown";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test starting a process instance, covering all edge-cases.
     */
    public void testStartCaseExceptions() throws Exception {

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Try starting without id and key
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with both id and key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");
        requestNode.put("processDefinitionKey", "456");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with both message and key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");
        requestNode.put("message", "456");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting process definition key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionKey", "123");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting process definition id
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting message
        requestNode = objectMapper.createObjectNode();
        requestNode.put("message", "unexistingmessage");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }
    
    /**
     * Test getting a list of case instance, using all possible filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testGetCaseInstancesByActivePlanItemDefinitionId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        String id = caseInstance.getId();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?activePlanItemDefinitionId=task1";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?activePlanItemDefinitionId=task2";
        assertResultsPresentInDataResponse(url);

        Task task = taskService.createTaskQuery().caseInstanceId(id).singleResult();
        taskService.complete(task.getId());
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?activePlanItemDefinitionId=task2";
        assertResultsPresentInDataResponse(url, id);
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?activePlanItemDefinitionId=task1";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test  bulk deletion of case instances.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testBulkDeleteCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.SEGMENT_RUNTIME_RESOURCES + "/" + CmmnRestUrls.SEGMENT_CASE_INSTANCE_RESOURCE + "/delete";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId());
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));
        // Check if case-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isZero();
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isZero();
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance3.getId())).isNotNull();
    }

    /**
     * Test  bulk deletion of case instances.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testInvalidBulkDeleteCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.SEGMENT_RUNTIME_RESOURCES + "/" + CmmnRestUrls.SEGMENT_CASE_INSTANCE_RESOURCE + "/delete";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId()).add("notValidID");
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));
        // Check if case-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance3.getId()).count()).isEqualTo(1);

        body = objectMapper.createObjectNode();
        body.put("action", "delete");
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

    }

    /**
     * Test bulk termination of case instances.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testBulkTerminateCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.SEGMENT_RUNTIME_RESOURCES + "/" + CmmnRestUrls.SEGMENT_CASE_INSTANCE_RESOURCE + "/delete";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "terminate");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId());
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));
        // Check if case-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isZero();
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isZero();
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance3.getId()).singleResult()).isNotNull();
    }

    /**
     * Test bulk termination of case instances.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testInvalidBulkTerminateCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.SEGMENT_RUNTIME_RESOURCES + "/" + CmmnRestUrls.SEGMENT_CASE_INSTANCE_RESOURCE + "/delete";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "terminate");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId()).add("notValidID");
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));
        // Check if case-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(1);
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance3.getId()).count()).isEqualTo(1);

        body = objectMapper.createObjectNode();
        body.put("action", "terminate");
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        body = objectMapper.createObjectNode();
        body.put("action", "invalidAction");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId());
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }
}

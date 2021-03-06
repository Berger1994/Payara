/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.monitoring.web;

import static fish.payara.monitoring.web.ApiRequests.DataType.POINTS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.internal.api.Globals;

import fish.payara.monitoring.alert.Alert;
import fish.payara.monitoring.alert.AlertService;
import fish.payara.monitoring.alert.Circumstance;
import fish.payara.monitoring.alert.Condition;
import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.alert.Watch;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesAnnotation;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.store.MonitoringDataRepository;
import fish.payara.monitoring.web.ApiRequests.DataType;
import fish.payara.monitoring.web.ApiRequests.SeriesQuery;
import fish.payara.monitoring.web.ApiRequests.SeriesRequest;
import fish.payara.monitoring.web.ApiResponses.AlertsResponse;
import fish.payara.monitoring.web.ApiResponses.AnnotationData;
import fish.payara.monitoring.web.ApiResponses.CircumstanceData;
import fish.payara.monitoring.web.ApiResponses.ConditionData;
import fish.payara.monitoring.web.ApiResponses.RequestTraceResponse;
import fish.payara.monitoring.web.ApiResponses.SeriesResponse;
import fish.payara.monitoring.web.ApiResponses.WatchData;
import fish.payara.monitoring.web.ApiResponses.WatchesResponse;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.store.RequestTraceStoreInterface;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class MonitoringConsoleResource {

    private static final Logger LOGGER = Logger.getLogger(MonitoringConsoleResource.class.getName());

    private static <T> T getService(Class<T> type) {
        return Globals.getDefaultBaseServiceLocator().getService(type);
    }

    private static MonitoringDataRepository getDataStore() {
        return getService(MonitoringDataRepository.class);
    }

    public static AlertService getAlertService() {
        return getService(AlertService.class);
    }

    private static RequestTraceStoreInterface getRequestTracingStore() {
        return getService( RequestTracingService.class).getRequestTraceStore();
    }

    private static Series seriesOrNull(String series) {
        try {
            return new Series(series);
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Failed to parse series", e);
            return null;
        }
    }

    @GET
    @Path("/annotations/data/{series}/")
    public List<AnnotationData> getAnnotationsData(@PathParam("series") String series) {
        Series key = seriesOrNull(series);
        return key == null 
                ? emptyList()
                        : getDataStore().selectAnnotations(key).stream().map(AnnotationData::new).collect(toList());
    }

    @GET
    @Path("/series/data/{series}/")
    public SeriesResponse getSeriesData(@PathParam("series") String series) {
        return getSeriesData(new SeriesRequest(series));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/series/data/")
    public SeriesResponse getSeriesData(SeriesRequest request) {
        int length = request.queries.length;
        List<List<SeriesDataset>> data = new ArrayList<>(length);
        List<List<SeriesAnnotation>> annotations = new ArrayList<>(length);
        List<Collection<Watch>> watches = new ArrayList<>(length);
        List<Collection<Alert>> alerts = new ArrayList<>(length);
        MonitoringDataRepository dataStore = getDataStore();
        AlertService alertService = getAlertService();
        for (SeriesQuery query : request.queries) {
            Series key = seriesOrNull(query.series);
            List<SeriesDataset> queryData = key == null || query.excludes(DataType.POINTS) //
                    || Series.ANY.equalTo(key) && query.truncates(POINTS) // if all alerts are requested don't send any particular data
                    ? emptyList()
                    : dataStore.selectSeries(key, query.instances);
            List<SeriesAnnotation> queryAnnotations = key == null || query.excludes(DataType.ANNOTATIONS) 
                    ? emptyList()
                    : dataStore.selectAnnotations(key, query.instances);
            Collection<Watch> queryWatches = key == null || query.excludes(DataType.WATCHES) 
                    ? emptyList()
                    : alertService.wachtesFor(key);
            Collection<Alert> queryAlerts = key == null || query.excludes(DataType.ALERTS) 
                    ? emptyList()
                    : alertService.alertsFor(key);
            data.add(queryData);
            watches.add(queryWatches);
            annotations.add(queryAnnotations);
            alerts.add(queryAlerts);
        }
        return new SeriesResponse(request.queries, data, annotations, watches, alerts,
                alertService.getAlertStatistics());
    }

    @GET
    @Path("/series/")
    public String[] getSeriesNames() {
        return stream(getDataStore().selectAllSeries().spliterator(), false)
                .map(dataset -> dataset.getSeries().toString()).sorted().toArray(String[]::new);
    }

    @GET
    @Path("/instances/")
    public String[] getInstanceNames() {
        return getDataStore().instances().toArray(new String[0]);
    }

    @GET
    @Path("/trace/data/{series}/")
    public List<RequestTraceResponse> getTraceData(@PathParam("series") String series) {
        String group = series.split(""+Series.TAG_SEPARATOR)[1].substring(2);
        List<RequestTraceResponse> response = new ArrayList<>();
        for (RequestTrace trace : getRequestTracingStore().getTraces()) {
            if (RequestTracingService.metricGroupName(trace).equals(group)) {
                response.add(new RequestTraceResponse(trace));
            }
        }
        return response;
    }

    @GET
    @Path("/alerts/data/")
    public AlertsResponse getAlertsData() {
        return new AlertsResponse(getAlertService().alerts());
    }

    @GET
    @Path("/alerts/data/{series}/")
    public AlertsResponse getAlertsData(@PathParam("series") String series) {
        return new AlertsResponse(getAlertService().alertsFor(seriesOrNull(series)));
    }

    @POST
    @Path("/alerts/ack/{serial}")
    public void acknowledgeAlert(@PathParam("serial") int serial) {
        Alert alert = getAlertService().alertBySerial(serial);
        if (alert != null) {
            alert.acknowledge();
        }
    }

    @GET
    @Path("/watches/data/")
    public WatchesResponse getWatchesData() {
        return new WatchesResponse(getAlertService().watches());
    }

    @DELETE
    @Path("/watches/data/{name}/")
    public Response deleteWatch(@PathParam("name") String name) {
        AlertService alertService = getAlertService();
        Watch watch = alertService.watchByName(name);
        if (watch != null) {
            alertService.removeWatch(watch);
        }
        return noContent();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/watches/data/")
    public Response createWatch(WatchData data) {
        if (data.name == null || data.name.isEmpty()) {
            return badRequest("Name missing");
        }
        Circumstance red = createCircumstance(data.red);
        Circumstance amber = createCircumstance(data.amber);
        Circumstance green = createCircumstance(data.green);
        if (red.start.isNone() && amber.start.isNone()) {
            return badRequest("A start condition for red or amber must be given");
        }
        Metric metric = Metric.parse(data.series, data.unit);
        Watch watch = new Watch(data.name, metric, false, red, amber, green);
        getAlertService().addWatch(watch);
        return noContent();
    }

    @PATCH
    @Path("/watches/data/{name}/")
    public Response patchWatch(@PathParam("name") String name, @QueryParam("disable") boolean disable) {
        return getAlertService().toggleWatch(name, disable) ? noContent() : notFound();
    }

    private static Circumstance createCircumstance(CircumstanceData data) {
        if (data == null) {
            return Circumstance.UNSPECIFIED;
        }
        Circumstance res = new Circumstance(fish.payara.monitoring.alert.Alert.Level.parse(data.level),
                createCondition(data.start), createCondition(data.stop));
        if (data.suppress != null) {
            res = res.suppressedWhen(Metric.parse(data.surpressingSeries, data.surpressingUnit),
                    createCondition(data.suppress));
        }
        return res;
    }

    private static Condition createCondition(ConditionData data) {
        if (data == null) {
            return Condition.NONE;
        }
        Condition res = new Condition(Operator.parse(data.operator), data.threshold);
        if (data.forMillis != null) {
            res = res.forLastMillis(data.forMillis.longValue());
        }
        if (data.forTimes != null) {
            res = res.forLastTimes(data.forTimes.intValue());
        }
        if (data.onAverage) {
            res = res.onAverage();
        }
        return res;
    }

    private static Response badRequest(String reason) {
        return Response.status(Status.BAD_REQUEST.getStatusCode(),reason).build();
    }

    private static Response noContent() {
        return Response.noContent().build();
    }

    private static Response notFound() {
        return Response.status(Status.NOT_FOUND).build();
    }
}

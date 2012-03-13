<%@page contentType="text/html;charset=UTF-8"%>
<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Delete bookmarked SPARQL queries">

    <stripes:layout-component name="contents">

        <h1>Bookmark this query:</h1>

        <div style="margin-top:20px">

             <strong>Query:</strong><pre><c:out value="${actionBean.query}"/></pre>
             <span><strong>Output format:&nbsp;</strong><c:out value="${actionBean.format}"/></span>
             <span style="padding-left:20px"><strong>Use inferencing:&nbsp;</strong><c:out value="${actionBean.useInferencing}"/></span>
             <span style="padding-left:20px"><strong>Hits per page:&nbsp;</strong><c:out value="${actionBean.nrOfHits}"/></span>
        </div>

        <crfn:form id="bookmarkQueryForm" action="/sparql" method="post">
            <c:if test="${not empty actionBean.defaultGraphUris}">
                <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                    <input type="hidden" name="default-graph-uri" value="${defaultGraphUri}" />
                </c:forEach>
            </c:if>
            <c:if test="${not empty actionBean.namedGraphUris}">
                <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                    <input type="hidden" name="named-graph-uri" value="${namedGraphUri}" />
                </c:forEach>
            </c:if>
            <div style="padding-top:20px">

                <label for="bookmarkNameText" class="question">Bookmark name:</label>
                <br/>
                <stripes:text name="bookmarkName" id="bookmarkNameText" size="100"/>
                <br/>

                <c:if test="${actionBean.sharedBookmarkPrivilege}">
                    <label for="sharedBookmarkCheck" class="question">Shared bookmark:</label>
                    <stripes:checkbox name="sharedBookmark" id="sharedBookmarkCheck" />
                    <br/>
                </c:if>

                <stripes:submit name="bookmark" value="Save"/>
                <br/>
                <span class="input-hint">Hint: use existing query's name to overwrite it</span>

                <stripes:hidden name="query" value="${actionBean.query}"/>
                <stripes:hidden name="format" value="${actionBean.format}"/>
                <stripes:hidden name="useInferencing" value="${actionBean.useInferencing}"/>
                <stripes:hidden name="nrOfHits" value="${actionBean.nrOfHits}"/>

            </div>
        </crfn:form>

    </stripes:layout-component>
</stripes:layout-render>

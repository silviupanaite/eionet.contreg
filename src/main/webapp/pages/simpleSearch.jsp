<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>	

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Simple search">

	<stripes:layout-component name="contents">

	<h1>Simple search</h1>
	<p>
	This page enables you to find content by case-insensitive text in any metadata element. For example:
	to search for content that contains words "air" or "soil" or both, enter <span class="searchExprSample">air soil</span>.
	Entering <span class="searchExprSample">"air pollution"</span> will search for the exact phrase "air pollution".
	<em>Words shorter than four letters are ignored!</em>
	</p>

	<stripes:form action="/simpleSearch.action" method="get" focus="searchExpression" style="padding-bottom:20px">
		<stripes:label for="expressionText" class="question">Expression</stripes:label>
		<stripes:text name="searchExpression" id="expressionText" size="50"/>
		<stripes:submit name="search" value="Search" id="searchButton"/>
		<stripes:text name="dummy" style="visibility:hidden;display:none" disabled="disabled" size="1"/>
	</stripes:form>
	<c:choose>
		<c:when test="${not empty param.search}">
			<stripes:layout-render name="/pages/common/subjectsResultList.jsp" tableClass="sortable"/>
		</c:when>
		<c:otherwise>
			<h2>Advanced search operators</h2>
			<dl>
				<dt><code class="literal">http://</code> or <code class="literal">https://</code></dt>
				<dd>
				   Any phrase that starts with a URL prefix is queried as an exact search.
				</dd>
				<dt><code class="literal">+</code></dt>
				<dd>
					A leading plus sign indicates that this word
					<span class="emphasis"><em>must</em></span> be present in each row that is
					returned.
				</dd>
				<dt><code class="literal">-</code></dt>
				<dd>
					A leading minus sign indicates that this word must
					<span class="emphasis"><em>not</em></span> be present in any of the rows that
					are returned. 
				</dd>
				<dd>
					Note: The <code class="literal">-</code> operator acts only to exclude
					rows that are otherwise matched by other search terms. Thus,
					a boolean-mode search that contains only terms preceded by
					<code class="literal">-</code> returns an empty result. It does not
					return “<span class="quote">all rows except those containing any of the
					excluded terms.</span>”
				</dd>
				<dt>(no operator)</dt>
				<dd>
					By default (when neither <code class="literal">+</code> nor
					<code class="literal">-</code> is specified) the word is optional
				</dd>
				<dt><code class="literal">&gt; &lt;</code></dt>
				<dd>
					These two operators are used to change a word's contribution
					to the relevance value that is assigned to a row. The
					<code class="literal">&gt;</code> operator increases the contribution
					and the <code class="literal">&lt;</code> operator decreases it.
				</dd>
				<dt><code class="literal">( )</code></dt>
				<dd>
					Parentheses group words into subexpressions. Parenthesized
					groups can be nested.
				</dd>
				<dt><code class="literal">~</code></dt>
				<dd>
					A leading tilde acts as a negation operator, causing the
					word's contribution to the row's relevance to be negative.
					This is useful for marking “<span class="quote">noise</span>” words. A row
					containing such a word is rated lower than others, but is
					not excluded altogether, as it would be with the
					<code class="literal">-</code> operator.
				</dd>
				<dt><code class="literal">*</code></dt>
				<dd>
					The asterisk serves as the truncation (or wildcard)
					operator. Unlike the other operators, it should be
					<span class="emphasis"><em>appended</em></span> to the word to be affected.
					Words match if they begin with the word preceding the
					<code class="literal">*</code> operator.
				</dd>
				<dd>
					If a stopword or too-short word is specified with the
					truncation operator, it will not be stripped from a boolean
					query. For example, a search for <code class="literal">'+word
					+stopword*'</code> will likely return fewer rows than a
					search for <code class="literal">'+word +stopword'</code> because the
					former query remains as is and requires
					<code class="literal">stopword*</code> to be present in a document.
					The latter query is transformed to <code class="literal">+word</code>.
				</dd>
				<dt><code class="literal">"</code></dt>
				<dd>
					A phrase that is enclosed within double quote
					(“<span class="quote"><code class="literal">"</code></span>”) characters matches
					only rows that contain the phrase <span class="emphasis"><em>literally, as it
					was typed</em></span>. The full-text engine splits the phrase
					into words, performs a search in the
					<code class="literal">FULLTEXT</code> index for the words. Nonword
					characters need not be matched exactly: Phrase searching
					requires only that matches contain exactly the same words as
					the phrase and in the same order. For example,
					<code class="literal">"test phrase"</code> matches <code class="literal">"test,
					phrase"</code>.
				</dd>
				<dd>
					If the phrase contains no words that are in the index, the
					result is empty. For example, if all words are either
					stopwords or shorter than the minimum length of indexed
					words, the result is empty.
				</dd>
			</dl>
		</c:otherwise>
	</c:choose>
</stripes:layout-component>
</stripes:layout-render>

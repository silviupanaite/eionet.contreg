package eionet.cr.dao.helpers;

import java.util.List;

import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;

/**
 *
 * @author jaanus
 *
 */
public abstract class FreeTextSearchHelper extends AbstractSearchHelper{

    /** */
    public enum FilterType { ANY_OBJECT, ANY_FILE, TEXTS, DATASETS, IMAGES, EXACT_MATCH };

    /** */
    protected FilterType filter = FilterType.ANY_OBJECT;

    /**
     *
     * @param pagingRequest
     * @param sortingRequest
     */
    public FreeTextSearchHelper(PagingRequest pagingRequest, SortingRequest sortingRequest) {
        super(pagingRequest, sortingRequest);
    }

    /**
     * @return
     */
    public FilterType getFilter() {
        return filter;
    }

    /**
     * @param filter
     */
    public void setFilter(FilterType filter) {
        this.filter = filter;
    }

    /*
     * (non-Javadoc)
     * @see eionet.cr.dao.helpers.AbstractSearchHelper#getOrderedQuery(java.util.List)
     */
    protected abstract String getOrderedQuery(List<Object> inParams);

    /*
     * (non-Javadoc)
     * @see eionet.cr.dao.helpers.AbstractSearchHelper#getUnorderedQuery(java.util.List)
     */
    public abstract String getUnorderedQuery(List<Object> inParams);

    /*
     * (non-Javadoc)
     * @see eionet.cr.dao.helpers.AbstractSearchHelper#getCountQuery(java.util.List)
     */
    public abstract String getCountQuery(List<Object> inParams);

    /*
     * (non-Javadoc)
     * @see eionet.cr.dao.helpers.AbstractSearchHelper#getMinMaxHashQuery(java.util.List)
     */
    public abstract String getMinMaxHashQuery(List<Object> inParams);
}

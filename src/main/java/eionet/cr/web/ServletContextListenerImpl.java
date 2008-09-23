package eionet.cr.web;

import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import eionet.cr.index.EncodingSchemes;
import eionet.cr.index.SubProperties;
import eionet.cr.index.walk.AllDocsWalker;
import eionet.cr.search.Searcher;

/**
 * 
 * @author heinljab
 *
 */
public class ServletContextListenerImpl implements ServletContextListener{

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent event) {
		
		AllDocsWalker.startupWalk();
	}
}

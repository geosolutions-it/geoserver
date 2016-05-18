/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.thoughtworks.xstream.XStream;

/**
 * Abstract Spring Batch {@link ItemReader}.
 * 
 * Configures the {@link Catalog} and initizializes the {@link XStreamPersister}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public abstract class CatalogReader<T> extends AbstractItemCountingItemStreamItemReader<T>
        implements ResourceAwareItemReaderItemStream<T>, InitializingBean {

    protected Class clazz;

    protected Backup backupFacade;

    protected Catalog catalog;

    protected XStreamPersister xstream;

    private XStream xp;

    public CatalogReader(Class<T> clazz, Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        this.clazz = clazz;
        this.backupFacade = backupFacade;
        this.xstream = xStreamPersisterFactory.createXMLPersister();

        this.setExecutionContextName(ClassUtils.getShortName(clazz));
    }

    @BeforeStep
    protected void retrieveInterstepData(StepExecution stepExecution) {
        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        //
        // For restore operations the order matters.
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.catalog = backupFacade.getRestoreExecutions().get(jobExecution.getId())
                    .getRestoreCatalog();
        } else {
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds();
        }

        Assert.notNull(catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        beforeStep(stepExecution);
    }

    protected abstract void beforeStep(StepExecution stepExecution);

    /**
     * @return the xp
     */
    public XStream getXp() {
        return xp;
    }

    /**
     * @param xp the xp to set
     */
    public void setXp(XStream xp) {
        this.xp = xp;
    }

}

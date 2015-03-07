package org.geoserver.vectortile;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;

import no.ecc.vectortile.*;
import static org.geoserver.vectortile.VectorTile.*;

public class VectorTileGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

	public VectorTileGetFeatureOutputFormat(GeoServer gs) {
		super(gs, Sets.union(Sets.newHashSet(MIME_TYPE), Sets.newHashSet(NAMES)));
	}
	@Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        return MIME_TYPE;
    }

    @Override
    public String getCapabilitiesElementName() {
        return NAMES.iterator().next();
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return Lists.newArrayList(NAMES);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);

        return Joiner.on("_").join(Iterables.transform(req.getQueries(), new Function<Query,String>() {
            @Override
            public String apply(Query input) {
                return input.getTypeNames().get(0).getLocalPart();
            }
        })) + "." + EXTENSION;
    }

	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation arg2) throws IOException, ServiceException {
		VectorTile vectortile = new VectorTile();
		VectorTileEncoder encoder = new VectorTileEncoder();
		String layerName = "Unknown Layer Name";
		for (FeatureCollection collection: featureCollection.getFeatures()) {
			if (! (collection instanceof SimpleFeatureCollection)) {
                throw new ServiceException("VectorTile OutputFormat does not support Complex Features.");
            }

            SimpleFeatureCollection features = (SimpleFeatureCollection)  collection;
            FeatureTypeInfo meta = lookupFeatureType(features);
            if (meta != null) {
               layerName = meta.getTitle();
            }else{
            	layerName = "Unknown Layer Name";
            }
		// Add one or more features with a layer name, a Map with attributes and a JTS Geometry. 
		// The Geometry uses (0,0) in lower left and (256,256) in upper right.
            SimpleFeatureIterator it = features.features();
            while (it.hasNext()){
            	SimpleFeature sf = it.next();
            	Collection<Property>properties = sf.getProperties();
            	Map<String,Object>attributes = propertiesToAttributes(properties);
            	Geometry geometry = (Geometry) sf.getDefaultGeometry();
            	encoder.addFeature(layerName, attributes, geometry);
            }
			
		}

		// Finally, get the byte array
		byte[] encoded = encoder.encode();
		vectortile.add(encoded);
		
        InputStream temp = new FileInputStream(vectortile.getFile());
        
        IOUtils.copy(temp, output);
        output.flush();        
        temp.close();
        vectortile.getFile().delete();
	}
	
    FeatureTypeInfo lookupFeatureType(SimpleFeatureCollection features) {
        FeatureType featureType = features.getSchema();
        if (featureType != null) {
            Catalog cat = gs.getCatalog();
            FeatureTypeInfo meta = cat.getFeatureTypeByName(featureType.getName());
            if (meta != null) {
                return meta;
            }

            LOGGER.fine("Unable to load feature type metadata for: " + featureType.getName());
        }
        else {
            LOGGER.fine("No feature type for collection, unable to load metadata");
        }

        return null;
    }

    String abstractOrDescription(FeatureTypeInfo meta) {
        return meta.getAbstract() != null ? meta.getAbstract() : meta.getDescription();
    }
    
    Map<String,Object>propertiesToAttributes(Collection<Property>properties){
    	Map<String,Object>attributes=new HashMap<String,Object>();
    	Iterator<Property>it = properties.iterator();
    	while(it.hasNext()){
    		Property property = it.next();
    		attributes.put(property.getName().getLocalPart(), property.getValue());
    	}
    	return attributes;
    	
    }

}

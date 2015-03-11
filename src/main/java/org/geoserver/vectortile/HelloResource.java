package org.geoserver.vectortile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;


public class HelloResource extends AbstractResource {
	private final GeoServer geoserver;
	private HelloResource(GeoServer geoserver){
        this.geoserver = geoserver;

	}
   @Override
   protected List<DataFormat> createSupportedFormats(Request request, Response response) {

	   List<DataFormat> formats = new ArrayList();
	   formats.add(new StringFormat( MediaType.TEXT_PLAIN ));

	   return formats;
   }
   @Override
   public void handleGet() {
      //get the appropriate format
      DataFormat format = getFormatGet();
      String xs = RESTUtils.getAttribute(getRequest(), "x");
      String ys = RESTUtils.getAttribute(getRequest(), "y");
      String zs = RESTUtils.getAttribute(getRequest(), "z");
      int x = Integer.parseInt(xs);
      int y = Integer.parseInt(ys);
      int z = Integer.parseInt(zs);
      String name = RESTUtils.getAttribute(getRequest(), "name");
      final Catalog catalog = geoserver.getCatalog();
      FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
      try {
		FeatureSource<? extends FeatureType, ? extends Feature> source = 
		          fti.getFeatureSource(null,null);
       // FeatureCollection<? extends FeatureType, ? extends Feature> features = source.getFeatures(filter);
		FeatureCollection fc = grabFeaturesInBoundingBox(source,z,x,y);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      

      //transform the string "Hello World" to the appropriate response
      getResponse().setEntity(format.toRepresentation("Hello World: "+x));
   }
   
   SimpleFeatureCollection grabFeaturesInBoundingBox(FeatureSource featureSource, int z, int x, int y)
	        throws Exception {
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    FeatureType schema = featureSource.getSchema();
	    
	    // usually "THE_GEOM" for shapefiles
	    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
	    //CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor()
	          //  .getCoordinateReferenceSystem();
	    CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:900913");
	    
	    
	    
	    //ReferencedEnvelope bbox = new ReferencedEnvelope(x1, y1, x2, y2, targetCRS);
	    ReferencedEnvelope bbox = tileAddressToBBox(z,x,y, targetCRS);
	    
	    Filter filter = ff.bbox(ff.property(geometryPropertyName), bbox);
	    return (SimpleFeatureCollection) featureSource.getFeatures(filter);
	}
private ReferencedEnvelope tileAddressToBBox(int z, int x, int y, CoordinateReferenceSystem targetCRS) {
	int x1 = 0;
	int y1=0;
	int x2=0;
	int y2=0;
	/* From http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
	 * minx, miny = self.PixelsToMeters( tx*self.tileSize, ty*self.tileSize, zoom )
	        maxx, maxy = self.PixelsToMeters( (tx+1)*self.tileSize, (ty+1)*self.tileSize, zoom )
	        return ( minx, miny, maxx, maxy ) 
	        
	  def PixelsToMeters(self, px, py, zoom):
        "Converts pixel coordinates in given zoom level of pyramid to EPSG:900913"

        res = self.Resolution( zoom )
        mx = px * res - self.originShift
        my = py * res - self.originShift
        return mx, my
        
               "Initialize the TMS Global Mercator pyramid"
        self.tileSize = 256
        self.initialResolution = 2 * math.pi * 6378137 / self.tileSize
        # 156543.03392804062 for tileSize 256 pixels
        self.originShift = 2 * math.pi * 6378137 / 2.0
        # 20037508.342789244
	        */
    return new ReferencedEnvelope(x1, y1, x2, y2, targetCRS);

}

}

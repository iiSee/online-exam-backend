package edu.sandau.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import edu.sandau.security.RequestFilter;
import edu.sandau.security.ResponseFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestConfig extends ResourceConfig {
	private final static Logger LOGGER = LoggerFactory.getLogger(RestConfig.class);

    public RestConfig()
    {
    	super();
    	this.init();
    }
    
    public void init()
    {
		LOGGER.info("load restful services......");
    	String packages = "sd.order.rest";
    	packages = this.loadPackages();
		LOGGER.info("in packages=" + packages);
    	this.packages(packages);
		this.register(MultiPartFeature.class);
		this.register(TestReaderIntercetor.class);
		this.register(io.swagger.jaxrs.listing.ApiListingResource.class);
		this.register(io.swagger.jaxrs.listing.SwaggerSerializers.class);

		this.register(RequestFilter.class);
		this.register(ResponseFilter.class);
	}
    
    public String loadPackages()
    {
    	InputStream is=this.getClass().getClassLoader().getResourceAsStream("restconfig.properties");
    	if(is!=null)
    	{
    		Properties p=new Properties();
    		try {
				p.load(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error("无法加载restconfig.properties文件。。。。。。");
				e.printStackTrace();
			}
    		String ret=p.getProperty("service_packages","sd.order.rest");
    		return ret;
    	}
    	return "sd.order";
    }
    
    public static class GzipInterceptor  implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context)
                throws IOException, WebApplicationException {

        	
            MultivaluedMap<String, Object> headers = context.getHeaders();
            
            headers.add("Content-Encoding", "gzip");
            
            String ContentType = context.getMediaType().toString();
            
            headers.add("Content-Type",ContentType+";charset=utf-8");//解决乱码问题
            
            final OutputStream gzipStream = new GZIPOutputStream(context.getOutputStream());
            context.setOutputStream(gzipStream);
            context.proceed();
            
            gzipStream.close();
            
            //LOGGER.info("GZIP拦截器压缩");
        }
    }
    
    public static class TestReaderIntercetor implements ReaderInterceptor{

		@Override
		public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
			// TODO Auto-generated method stub
			MultivaluedMap<String, String> headers=context.getHeaders();
			
			java.util.Iterator<String>  keys=headers.keySet().iterator();
			String log = "request filter:{\n";
			while(keys.hasNext())
			{
				String key =keys.next();
				String v=headers.getFirst(key);
				log += "     " + key + "=" + v + "\n";
			}
			log += "}";
			LOGGER.info(log);
			return context.proceed();
		}
    	
    }
}
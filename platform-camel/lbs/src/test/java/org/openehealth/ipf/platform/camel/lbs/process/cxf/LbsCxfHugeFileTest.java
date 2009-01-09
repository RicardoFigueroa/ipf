/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.platform.camel.lbs.process.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.message.MessageContentsList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openehealth.ipf.commons.lbs.attachment.AttachmentFactory;
import org.openehealth.ipf.commons.lbs.store.DiskStore;
import org.openehealth.ipf.commons.lbs.store.FileSystemLayoutStrategy;
import org.openehealth.ipf.commons.lbs.store.FlatFileSystemLayout;
import org.openehealth.ipf.commons.lbs.store.FlatUriUuidConversion;
import org.openehealth.ipf.commons.lbs.store.UuidUriConversionStrategy;
import org.openehealth.ipf.platform.camel.lbs.builder.RouteBuilder;
import org.openehealth.ipf.platform.camel.test.junit.DirtySpringContextJUnit4ClassRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * This test creates CXF endpoints implicitly via the routes. These will
 * not automatically be removed after the test execution. To do this it is
 * necessary to dirty the Spring application context. The best way to do this
 * is to run this test via {@link DirtySpringContextJUnit4ClassRunner}.
 * <b><p>
 * Do not simply copy this code. It could result it bad performance of tests.
 * Use the standard {@link SpringJUnit4ClassRunner} if you don't need this, 
 * which is usually the case.
 * </b><p>
 * This class requires that the Spring application context is recreated because
 * it creates HTTP endpoints. These endpoints will not be thrown away and use 
 * the tcp/ip ports and endpoint names. Subsequent quests could fail because
 * the ports are in use and exchanges might not be received by the correct 
 * endpoint.
 * @author Jens Riemschneider
 */
@RunWith(DirtySpringContextJUnit4ClassRunner.class) // DO NOT SIMPLY COPY!!! see above
@ContextConfiguration(locations = { "/context-lbs-huge-file.xml" })
public class LbsCxfHugeFileTest {

    private static final String ENDPOINT = 
        "http://localhost:9000/SoapContext/HugeFilePort";

    private Greeter greeter;
    private BindingProvider provider;

    private static final URI baseUri = URI.create("http://localhost/");

    public static final long CONTENT_SIZE = 1024 * 1024 * 100 + 5;
    private File baseDir;

    private AttachmentFactory factory;

    private DiskStore store;

    @Autowired
    private CamelContext camelContext;

    @Before
    public void setUp() throws Exception {
        File temp = File.createTempFile(getClass().getName(), "");
        temp.delete();
        baseDir = new File(temp.getAbsolutePath());
        baseDir.mkdir();
        
        FileSystemLayoutStrategy layout = new FlatFileSystemLayout(baseDir);
        UuidUriConversionStrategy conversion = new FlatUriUuidConversion(baseUri);
        store = new DiskStore(layout, conversion);
        factory = new AttachmentFactory(store, "default");

        URL wsdlResource = getClass().getClassLoader().getResource("hello_world.wsdl");
        SOAPService service = new SOAPService(wsdlResource);
        greeter = service.getSoapOverHttp();
        provider = (BindingProvider)greeter;
        SOAPBinding binding = (SOAPBinding) provider.getBinding(); 
        binding.setMTOMEnabled(true);
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
        
        camelContext.addRoutes(createRouteBuilder());
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.deleteAll();
            if (baseDir.exists()) {
                baseDir.setWritable(true);
                FileUtils.deleteDirectory(baseDir);
            }
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("cxf:bean:soapEndpointHugeFile?dataFormat=POJO")
                    .intercept(store().with(factory))
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {                            
                            MessageContentsList inParams = exchange.getIn().getBody(MessageContentsList.class);
                            Holder<String> name = (Holder<String>) inParams.get(0);
                            Holder<DataHandler> attachinfo = (Holder<DataHandler>) inParams.get(1);                            
                            DataHandler onewayattach = (DataHandler) inParams.get(2);
                            
                            name.value = "resultText";
                            attachinfo.value = onewayattach;
                            Object[] outParams = { null, name, attachinfo };
                            exchange.getOut().setBody(outParams);
                        }                        
                    });
            }            
        };
    }

    @Test
    public void testHugeFile() throws Exception {
        DataSource dataSourceAttachInfo = new ByteArrayDataSource("Smaller content", "application/octet-stream");
        DataHandler dataHandlerAttachInfo = new DataHandler(dataSourceAttachInfo);
        Holder<DataHandler> handlerHolderAttachInfo = new Holder(dataHandlerAttachInfo);

        DataSource dataSourceOneWay = new InputStreamDataSource();
        DataHandler dataHandlerOneWay = new DataHandler(dataSourceOneWay);
        
        Holder<String> nameHolder = new Holder("Hello Camel!!");
        greeter.postMe(nameHolder, handlerHolderAttachInfo, dataHandlerOneWay);
        
        assertEquals("resultText", nameHolder.value);
        InputStream resultInputStream = handlerHolderAttachInfo.value.getInputStream();
        try {
            assertTrue(IOUtils.contentEquals(new HugeContentInputStream(), resultInputStream));
        }
        finally {
            resultInputStream.close();
        }
    }

    private static final class HugeContentInputStream extends InputStream {
        private long readBytes;
        private int count;
        
        @Override
        public int read() throws IOException {
            if (readBytes == CONTENT_SIZE) {
                return -1;
            }
            ++readBytes;
            return 'L';
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            long sizeToRead = len;
            if (readBytes + sizeToRead > CONTENT_SIZE) {
                sizeToRead = Math.max(0, CONTENT_SIZE - readBytes);
            }
            
            Arrays.fill(b, off, (int)(off + sizeToRead), (byte)65);
            readBytes += sizeToRead;
            if (++count == 1000) {
                count = 0;
            }
            return (int) sizeToRead;
        }
        
        public long getReadBytes() {
            return readBytes;
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            assertEquals(CONTENT_SIZE, readBytes);
            readBytes = 0;
        }
    }

    private static final class InputStreamDataSource implements DataSource {
        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new HugeContentInputStream();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}

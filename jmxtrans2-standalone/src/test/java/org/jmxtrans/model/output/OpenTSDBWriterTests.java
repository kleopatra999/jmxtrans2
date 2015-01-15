/**
 * The MIT License
 * Copyright (c) 2014 JMXTrans Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jmxtrans.exceptions.LifecycleException;
import org.jmxtrans.model.Query;
import org.jmxtrans.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link OpenTSDBWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenTSDBWriter.class })
public class OpenTSDBWriterTests {
	protected OpenTSDBWriter writer;
	protected Query mockQuery;
	protected Result mockResult;
	protected Socket mockSocket;
	protected DataOutputStream mockOut;
	protected InputStreamReader mockInStreamRdr;
	protected BufferedReader mockBufRdr;
	protected Logger mockLog;
	protected ImmutableMap<String, Object> testValues;

	/**
	 * Prepare the test with standard mocks and mock interactions.  Also perform the base configuration of the
	 * object under test.
	 */
	@Before
	public void	setupTest () throws Exception {
		// Prepare Mock Objects
		this.mockQuery = mock(Query.class);
		this.mockResult = mock(Result.class);
		this.mockSocket = mock(Socket.class);
		this.mockInStreamRdr = mock(InputStreamReader.class);
		this.mockBufRdr = mock(BufferedReader.class);
		this.mockLog = mock(Logger.class);

		// PowerMockito mocks for those final/static classes and methods:
		this.mockOut = PowerMockito.mock(DataOutputStream.class);


		// Setup common mock interactions.
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenReturn(this.mockSocket);
		PowerMockito.whenNew(DataOutputStream.class).withAnyArguments().thenReturn(this.mockOut);
		PowerMockito.whenNew(InputStreamReader.class).withAnyArguments().thenReturn(this.mockInStreamRdr);
		PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(this.mockBufRdr);


		// When results are needed.
		testValues = ImmutableMap.<String, Object>of("x-att1-x", "120021");
		when(this.mockResult.getValues()).thenReturn(testValues);
		when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x");
		when(this.mockInStreamRdr.ready()).thenReturn(false);


		// Prepare the object under test and test data.
		this.writer = OpenTSDBWriter.builder()
				.setDebugEnabled(false)
				.setHost("localhost")
				.setPort(4242)
				.build();

		// Inject the mock logger
		Whitebox.setInternalState(OpenTSDBWriter.class, Logger.class, this.mockLog);
	}

	/**
	 * Test a successful send without any response from OpenTSDB.
	 */
	@Test
	public void	testSuccessfulSendNoOpenTSDBResponse () throws Exception {
		ArgumentCaptor<String>	lineCapture = ArgumentCaptor.forClass(String.class);

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();

		// Verify.
		verify(this.mockOut).writeBytes(lineCapture.capture());

		assertThat(lineCapture.getValue()).startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021");
		assertThat(lineCapture.getValue()).contains(" host=");
	}

	/**
	 * Verify a successful send with a response from OpenTSDB with the second ready() on the InputStream returning
	 * false.
	 */
	@Test
	public void	testSuccessfulSendOpenTSDBResponse1 () throws Exception {
		// Prepare.
		when(this.mockInStreamRdr.ready()).thenReturn(true).thenReturn(false);
		when(this.mockBufRdr.readLine()).thenReturn("X-OPENTSDB-MSG-X");

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();


		// Verify.
		verify(this.mockOut).writeBytes(Mockito.startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		verify(this.mockLog).warn("OpenTSDB says: X-OPENTSDB-MSG-X");
	}

	/**
	 * Verify a successful send with a response from OpenTSDB with the second readLine returning null.
	 */
	@Test
	public void	testSuccessfulSendOpenTSDBResponse2 () throws Exception {
		// Prepare.
		when(this.mockInStreamRdr.ready()).thenReturn(true);
		when(this.mockBufRdr.readLine()).thenReturn("X-OPENTSDB-MSG-X").thenReturn(null);

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();


		// Verify.
		verify(this.mockOut).writeBytes(Mockito.startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		verify(this.mockLog).warn("OpenTSDB says: X-OPENTSDB-MSG-X");
	}

	/**
	 * Test throwing of UnknownHostException when creating the socket.
	 */
	@Test
	public void	testUnknownHostException () throws Exception {
		// Prepare.
		UnknownHostException uhExc = new UnknownHostException("X-TEST-UHE-X");
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenThrow(uhExc);

		try {
			// Execute.
			this.writer.start();

			fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			assertThat(uhExc).isSameAs(lcExc.getCause());
			verify(this.mockLog).error(contains("opening socket"), eq(uhExc));
		}
	}

	/**
	 * Test throwing of IOException when creating the socket.
	 */
	@Test
	public void	testSocketCreationIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenThrow(ioExc);

		try {
			// Execute.
			this.writer.start();
			fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			assertThat(ioExc).isSameAs(lcExc.getCause());
			verify(this.mockLog).error(contains("opening socket"), eq(ioExc));
		}
	}

	/**
	 * Test throwing of IOException when closing the socket.
	 */
	@Test
	public void	testSocketCloseIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		doThrow(ioExc).when(this.mockSocket).close();

		// Execute.
		this.writer.start();
		try {
			this.writer.stop();
			fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			assertThat(ioExc).isSameAs(lcExc.getCause());
			verify(this.mockLog).error(contains("closing socket"), eq(ioExc));
		}
	}

	/**
	 * Test throwing of IOException when starting output.
	 */
	@Test
	public void	testStartOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		when(this.mockSocket.getOutputStream()).thenThrow(ioExc);

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			fail("IOException missing");
		} catch ( IOException ioCaught ) {
			// Verify.
			assertThat(ioExc).isSameAs(ioCaught);
			verify(this.mockLog).error(contains("output stream"), eq(ioExc));
		}
	}

	/**
	 * Test IOException thrown on SendOutput.
	 */
	@Test
	public void	testSendOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		doThrow(ioExc).when(this.mockOut).writeBytes(anyString());

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			fail("IOException missing");
		} catch ( IOException ioCaught ) {
			// Verify.
			assertThat(ioExc).isSameAs(ioCaught);
			verify(this.mockLog).error(contains("writing result"), eq(ioExc));
		}
	}

	@Test
	public void	testFinishOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		doThrow(ioExc).when(this.mockOut).flush();

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			fail("exception on flush was not thrown");
		} catch ( IOException ioCaught ) {
			// Verify.
			assertThat(ioExc).isSameAs(ioCaught);
			verify(this.mockLog).error(contains("flush failed"), eq(ioExc));
		}
	}

	@Test(expected = NullPointerException.class)
	public void	exceptionThrownIfHostIsNotDefined() throws Exception {
		OpenTSDBWriter.builder()
				.setPort(4242)
				.build();
	}

	@Test(expected = NullPointerException.class)
	public void	exceptionThrownIfPortIsNotDefined() throws Exception {
		OpenTSDBWriter.builder()
				.setHost("localhost")
				.build();
	}
}
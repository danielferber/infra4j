/*
 * Copyright 2012 Daniel Felix Ferber
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.slf4j;

import infra.slf4j.MeterEvent;
import infra.slf4j.Parser;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MeterEventTest {
	MeterEvent e1;
	MeterEvent e2;
	StringBuilder sb;
	Parser p1;
	Parser p2;

	@Before
	public void pre() {
		e1 = new MeterEvent();
		e2 = new MeterEvent();
		sb = new StringBuilder();
		p1 = new Parser();
		p2 = new Parser();
	}

	@After
	public void pos() {
		e1 = null;
		e2 = null;
		sb = null;
		p1 = null;
		p2 = null;
	}

	@Test
	public void test01() throws IOException {
		e1.name = "nome";

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test02() throws IOException {
		e1.name = "nome";
		e1.counter = 10;

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test03() throws IOException {
		e1.name = "nome";
		e1.createTime = 10;

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test04() throws IOException {
		e1.name = "nome";
		e1.createTime = 10;
		e1.startTime = 20;

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test05() throws IOException {
		e1.name = "nome";
		e1.createTime = 10;
		e1.startTime = 20;
		e1.stopTime = 30;

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test06() throws IOException {
		e1.name = "nome";
		e1.exceptionClass = EOFException.class.getName();

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test07() throws IOException {
		e1.name = "nome";
		e1.exceptionClass = EOFException.class.getName();
		e1.exceptionMessage = "Ixe, deu um erro.";

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test08() throws IOException {
		e1.name = "nome";
		e1.message = "Mensagem de teste.";

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test09() throws IOException {
		e1.name = "nome";
		e1.message = "Mensagem de teste.";
		e1.createTime = 10;
		e1.startTime = 20;
		e1.stopTime = 30;
		e1.exceptionClass = EOFException.class.getName();
		e1.exceptionMessage = "Ixe, deu um erro.";

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test10() throws IOException {
		e1.name = "nome";
		e1.context = new HashMap<String, String>();

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test11() throws IOException {
		e1.name = "nome";
		e1.context = new HashMap<String, String>();
		e1.context.put("nome", "daniel");
		e1.context.put("idade", "30");

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test12() throws IOException {
		e1.name = "nome";
		e1.context = new HashMap<String, String>();
		e1.context.put("nome", "daniel");
		e1.context.put("ok", null);

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

	@Test
	public void test13() throws IOException {
		e1.name = "nome";
		e1.context = new HashMap<String, String>();
		e1.context.put("hist", null);
		e1.context.put("ok", null);

		MeterEvent.writeToString(p1, e1, sb);
		MeterEvent.readFromString(p2, e2, sb.toString());

		Assert.assertEquals(e1, e2);
	}

}


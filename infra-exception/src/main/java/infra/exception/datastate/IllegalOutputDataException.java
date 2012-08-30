package infra.exception.datastate;
///*
// * Copyright 2012 Daniel Felix Ferber
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package infra.exception.assertions.datastate;
//
//
///**
// * Data that is being produced is not valid.
// * @author Daniel Felix Ferber
// */
//public class IllegalOutputDataException extends IllegalDataStateException {
//	private static final long serialVersionUID = 1L;
//
//	public IllegalOutputDataException() { super(); }
//	public IllegalOutputDataException(String message) { super(message); }
//
////	/** Raises exception if condition is false. */
////	public static boolean apply(boolean condition) {
////		if (!condition) throw new IllegalOutputDataException();
////		return true;
////	}
////	/** Raises exception if one condition is false. */
////	public static boolean apply(boolean ... conditions) {
////		for (boolean b : conditions) {
////			if (!b) throw new IllegalOutputDataException();
////		}
////		return true;
////	}
//}
/**
 * @author Alain Barbier
 * Copyright © 2026 Alain Barbier (alias Metroidzeta) - All rights reserved.
 *
 * This file is part of the project covered by the
 * "Educational and Personal Use License / Licence d’Utilisation Personnelle et Éducative".
 *
 * Permission is granted to fork and use this code for educational and personal purposes only.
 *
 * Commercial use, redistribution, or public republishing of modified versions
 * is strictly prohibited without the express written consent of the author.
 */
package core.loaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.regex.Pattern;

final class JsonUtil {

	private static final Pattern INTEGER_PATTERN = Pattern.compile("-?(0|[1-9]\\d*)");

	private JsonUtil() { throw new AssertionError("La classe JsonUtil ne doit pas être instanciée."); } // Empêche toute instanciation

	public static boolean isIntKey(JsonObject obj, String key) {
		if (obj == null || !obj.has(key)) return false;

		final JsonElement element = obj.get(key);

		return element.isJsonPrimitive()
				&& element.getAsJsonPrimitive().isNumber()
				&& INTEGER_PATTERN.matcher(element.getAsString()).matches();
	}

	public static boolean isStringKey(JsonObject obj, String key) {
		if (obj == null || !obj.has(key)) return false;

		final JsonElement element = obj.get(key);

		return element.isJsonPrimitive()
				&& element.getAsJsonPrimitive().isString();
	}

	public static boolean isArrayKey(JsonObject obj, String key) {
		return obj != null && obj.has(key) && obj.get(key).isJsonArray();
	}

	public static boolean isNullableStringKey(JsonObject object, String key) {
		if (object == null || !object.has(key)) return false;

		final JsonElement element = object.get(key);

		return element.isJsonNull() || (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString());
	}
}
package net.timothaty.timothatystrinkets.client.compat;

import net.neoforged.fml.ModList;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

public final class FirstPersonModelCompat {
	private static final String FIRST_PERSON_MODEL_MOD_ID = "firstperson";
	private static final String FIRST_PERSON_MODEL_CORE_CLASS =
			"dev.tr7zw.firstperson.FirstPersonModelCore";
	private static final String FIRST_PERSON_MODEL_API_CLASS =
			"dev.tr7zw.firstperson.api.FirstPersonAPI";
	private static Boolean firstPersonModelPresent;
	private static boolean lookedUpApiEnabledMethod;
	private static Method apiEnabledMethod;

	private FirstPersonModelCompat() {
	}

	public static boolean isTrueFirstPersonActive() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.options.getCameraType() == CameraType.FIRST_PERSON
				&& isFirstPersonModelPresent()
				&& isFirstPersonModelEnabled();
	}

	private static boolean isFirstPersonModelPresent() {
		if (firstPersonModelPresent == null) {
			firstPersonModelPresent = ModList.get()
					.isLoaded(FIRST_PERSON_MODEL_MOD_ID)
					|| classExists(FIRST_PERSON_MODEL_CORE_CLASS)
					|| classExists(FIRST_PERSON_MODEL_API_CLASS);
		}
		return firstPersonModelPresent;
	}

	private static boolean isFirstPersonModelEnabled() {
		Method method = getApiEnabledMethod();
		if (method == null)
			return true;

		try {
			Object value = method.invoke(null);
			return !(value instanceof Boolean enabled) || enabled;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return true;
		}
	}

	private static Method getApiEnabledMethod() {
		if (lookedUpApiEnabledMethod)
			return apiEnabledMethod;

		lookedUpApiEnabledMethod = true;
		try {
			Class<?> apiClass = Class.forName(
					FIRST_PERSON_MODEL_API_CLASS,
					false,
					FirstPersonModelCompat.class.getClassLoader()
			);
			apiEnabledMethod = apiClass.getMethod("isEnabled");
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			apiEnabledMethod = null;
		}
		return apiEnabledMethod;
	}

	private static boolean classExists(String className) {
		try {
			Class.forName(
					className,
					false,
					FirstPersonModelCompat.class.getClassLoader()
			);
			return true;
		} catch (ClassNotFoundException | LinkageError ignored) {
			return false;
		}
	}
}

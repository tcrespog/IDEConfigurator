package es.gavab.ideconfigurator;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.BasicSplashHandler;

public class ExtensibleSplashHandler
        extends BasicSplashHandler {

    public void init(Shell splash) {
        super.init(splash);
        String progressRectString = null;
        String messageRectString = null;
        String foregroundColorString = null;
        IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product.getProperty("startupProgressRect");
            messageRectString = product.getProperty("startupMessageRect");
            foregroundColorString = product.getProperty("startupForegroundColor");
        }
        Rectangle progressRect = StringConverter.asRectangle(
                progressRectString, new Rectangle(10, 10, 300, 15));
        setProgressRect(progressRect);

        Rectangle messageRect = StringConverter.asRectangle(messageRectString,
                new Rectangle(10, 35, 300, 15));
        setMessageRect(messageRect);
        int foregroundColorInteger;
        try {
            foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
        } catch (Exception localException) {
            foregroundColorInteger = 13817855;
        }
        setForeground(new RGB((foregroundColorInteger & 0xFF0000) >> 16,
                (foregroundColorInteger & 0xFF00) >> 8,
                foregroundColorInteger & 0xFF));

        getContent();
    }
}

package modelo.entidades.sms;

import controlador.General;
import controlador.sms.EnviarSMS;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import modelo.Dominios.EstatusSMS;
import modelo.HibernateUtil;
import modelo.entidades.auditoria.AuditoriaBasica;
import modelo.entidades.personas.maestra.Persona;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.proxy.pojo.javassist.JavassistProxyFactory;
import org.openswing.swing.mdi.client.MDIFrame;

/**
 *
 * @author adrian
 */
public class SMSEntradaActualizar {

    public void Actualizar() {
        boolean hay = EnviarSMS.hayInternetHost2();
        if (!hay) {
            JOptionPane.showMessageDialog(MDIFrame.getInstance(), "No hay Internet", "Mensaje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            String usuario = java.net.URLEncoder.encode(General.empresa.getNombre(), "ISO-8859-1");
            URL url = new URL("http://desolinfor.comze.com//smsbox/index.php?usuario=" + usuario + "&inbox=1");
            System.out.println("read url");
            BufferedReader b = new BufferedReader(new InputStreamReader(url.openStream()));
            String l = b.readLine().trim();
            if (l.indexOf("<br /><br />") != -1) {
                String datos[] = l.split("<br /><br />");
                Transaction tx = s.beginTransaction();
                for (int i = 0; i < datos.length; i++) {
                    String[] campo = datos[i].split("<br />");
                    SMSEntrada en = new SMSEntrada();
                    en.setPara(campo[0]);
                    en.setDe(campo[1]);
                    en.setTexto(campo[2]);
                    en.setTelefono(campo[3]);
                    SimpleDateFormat formatoDelTexto = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String strFecha = campo[4];
                    try {
                        en.setFecha(formatoDelTexto.parse(strFecha));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    SMS sms = new SMS(en.getDe(), en.getPara(), en.getTelefono(),
                            "Recibido", en.getTexto(), en.getFecha(),
                            EstatusSMS.RECIBIDO,
                            new AuditoriaBasica(new Date(),
                            General.usuario.getUserName(),
                            Boolean.TRUE));
                    String numero = en.getTelefono();
                    if (numero.startsWith("0") && numero.length() == 11) {
                        numero = numero.replaceFirst("0", "");
                    }
                    List<Persona> list = s.createQuery("SELECT DISTINCT C FROM " + Persona.class.getName() + " C "
                            + "JOIN C.telefonos T WHERE T.numeroCompleto=?").setString(0, numero).list();
                    for (Persona persona : list) {
                        persona.getSmss().add(sms);
                        s.update(persona);
                    }
                    s.save(en);
                }
                tx.commit();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            s.close();
        }
    }
}

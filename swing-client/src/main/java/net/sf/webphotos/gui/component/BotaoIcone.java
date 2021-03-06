/**
 * Copyright 2008 WebPhotos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.webphotos.gui.component;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import net.sf.webphotos.action.AcaoToolbar;

/**
 * <PRE>
 * Base para gera��o de Bot�es iconificados
 * (um s�mbolo visual no lugar de uma palavra para identificar o bot�o)
 *
 * Atualmente ele � base para quatro bot�es da tela principal do WebPhotos abaixo do
 * espa�o usado para por legenda e cr�dito.
 * </PRE>
 */
public class BotaoIcone extends JButton {

    private static final Action acaoToolbar = new AcaoToolbar();
    private String iconPrefix = "webfotos";
    /**
     * Constante que define a largura do Bot�o-�cone
     */
    public static final int TAMANHOX = 24;
    /**
     * Constante que define a altura do Bot�o-�cone
     */
    public static final int TAMANHOY = 24;
    /**
     * Par�metro que define se o bot�o ira trabalhar com suas bordas de forma
     * personalizada.
     */
    //protected static boolean botaoPersonalizado = Util.getConfig().getBoolean("BotoesPersonalizados");
    protected static boolean botaoPersonalizado = false;

    /**
     * Construtor Padr�o do Bot�o-�cone.
     */
    public BotaoIcone() {

        this.setActionCommand(iconPrefix);
        this.setRolloverEnabled(true);
        this.addActionListener(acaoToolbar);
        
        this.setText("");

        if (botaoPersonalizado) {
            this.setContentAreaFilled(false);
            this.setBorderPainted(false);
            this.setFocusPainted(false);
        }

        this.setBounds(getX(), getY(), TAMANHOX, TAMANHOY);
        
        this.setUpIcons(iconPrefix);

    }

    /**
     * Retorna o Objeto do tipo {@link net.sf.webphotos.acao.AcaoToolbar}
     * respons�vel pelas a��es que todos os bot�es ir�o acessar.
     *
     * @return Objeto {@link net.sf.webphotos.acao.AcaoToolbar} atualmente em
     * uso.
     */
    public static Action getAcaoToolbar() {
        return acaoToolbar;
    }

    /**
     * Retorna o Prefixo dos �cones em uso.
     *
     * @return String em uso.
     */
    public String getIconPrefix() {
        return iconPrefix;
    }

    /**
     * <PRE>
     * Prefixo do nome dos 3 �cones que ser�o usados nas bordas personalizadas.
     * Exemplo:
     *
     * Com o prefixo icone1, temos:
     *
     * * icone1.gif - Padr�o. apresentado durante toda a execu��o.
     * * icone1over.gif - Apresentado quando o mouse passa sobre o bot�o.
     * * icone1press.gif - Apresentado quando o mouse � pressionado.
     * </PRE>
     *
     * @param iconPrefix A string que ser� usada.
     */
    public synchronized void setIconPrefix(String iconPrefix) {
        this.iconPrefix = iconPrefix;
        setUpIcons(iconPrefix);
    }

    /**
     * @param iconPrefix Icon Set prefix
     */
    private void setUpIcons(String iconPrefix) {
        this.setIcon(new ImageIcon(getClass().getResource("/icons/" + iconPrefix + ".gif")));
        this.setActionCommand(iconPrefix);
        if (botaoPersonalizado) {
            this.setRolloverIcon(new ImageIcon(getClass().getResource("/icons/" + iconPrefix + "over.gif")));
            this.setPressedIcon(new ImageIcon(getClass().getResource("/icons/" + iconPrefix + "press.gif")));
        }
    }
}
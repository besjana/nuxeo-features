package org.nuxeo.opensocial.container.client.presenter;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.Presenter;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.gadgets.facets.IsCollapsable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsMaximizable;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.HasFacets;
import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DefaultPortletPreference;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * @author Stéphane Fourrier
 */
public class PortletPresenter extends WidgetPresenter<PortletPresenter.Display> {
    public interface Display extends WidgetDisplay, HasMouseDownHandlers,
            HasMouseUpHandlers, HasMouseMoveHandlers, HasMouseOutHandlers,
            HasId {
        void showContent();

        void hideContent();

        void clean();

        Widget getHeader();

        String getParentId();

        void setHeight(long height);

        void setTitle(String title);

        void addTool(Widget widget);

        void setBorderColor(String color);

        void setHeaderColor(String color);

        void setTitleColor(String color);

        void addContent(Widget widget);

        void setIcon(String icon);
    }

    private WebContentData data;
    private Presenter contentPresenter;

    @Inject
    public PortletPresenter(final Display display, final EventBus eventBus,
            WebContentData data, Presenter contentPresenter) {
        super(display, eventBus);

        this.data = data;
        this.contentPresenter = contentPresenter;

        fetchLayoutContent();
    }

    private void fetchLayoutContent() {
        display.addContent(((WidgetDisplay) contentPresenter.getDisplay()).asWidget());

        display.setBorderColor(data.getPreferences()
                .get(DefaultPortletPreference.WC_BORDER_COLOR.toString()));

        display.setHeaderColor(data.getPreferences()
                .get(DefaultPortletPreference.WC_HEADER_COLOR.toString()));

        display.setTitleColor(data.getPreferences()
                .get(DefaultPortletPreference.WC_TITLE_COLOR.toString()));

        display.setId(data.getId());

        display.setHeight(data.getHeight());

        if (data.isCollapsed()) {
            display.hideContent();
        }

        display.setIcon(data.getIcon());

        final HasFacets content = ((HasFacets) ((WidgetDisplay) contentPresenter.getDisplay()).asWidget());
        for (final Facet facet : content.getFacets()) {
            display.addTool(facet.asWidget());

            facet.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    registerMaximizeFacetInteroperabilityEvent();

                    HasId eventToFire = (HasId) facet.getEventToFire();
                    eventToFire.setId(data.getId());
                    facet.changeState();

                    eventBus.fireEvent((GwtEvent<?>) eventToFire);
                }

                private void registerMaximizeFacetInteroperabilityEvent() {
                    if (facet instanceof IsMaximizable) {
                        for (final Facet f : content.getFacets()) {
                            if (f != facet) {
                                if (facet.isInFirstState()) {
                                    f.disable();
                                } else {
                                    f.enable();
                                }
                                // If we maximize the gadget and if it is
                                // collapsed we have to uncollapse it !
                                if (f instanceof IsCollapsable
                                        && !f.isInFirstState()) {
                                    HasId eventToFire = (HasId) f.getEventToFire();
                                    eventToFire.setId(data.getId());

                                    f.changeState();

                                    eventBus.fireEvent((GwtEvent<?>) eventToFire);
                                }
                            }
                        }
                    }
                }
            });

            // Set the default behavior for the portlet if it is collapsable
            if (facet instanceof IsCollapsable && data.isCollapsed()) {
                facet.changeState();
            }
        }

        display.setTitle(data.getTitle());
    }

    public void setBorderColor(String color) {
        display.setBorderColor(color);
    }

    public void setHeaderColor(String color) {
        display.setHeaderColor(color);
    }

    public void setTitleColor(String color) {
        display.setTitleColor(color);
    }

    public String getTitleColor() {
        return data.getPreferences()
                .get(DefaultPortletPreference.WC_TITLE_COLOR.toString());
    }

    public String getBorderColor() {
        return data.getPreferences()
                .get(DefaultPortletPreference.WC_BORDER_COLOR.toString());
    }

    public String getHeaderColor() {
        return data.getPreferences()
                .get(DefaultPortletPreference.WC_HEADER_COLOR.toString());
    }

    public void setTitle(String title) {
        display.setTitle(title);
    }

    public String getTitle() {
        return data.getTitle();
    }

    public void showContent() {
        if (!data.isCollapsed())
            display.showContent();
    }

    public void hideContent() {
        display.hideContent();
    }

    public void collapse() {
        display.hideContent();
    }

    public void uncollapse() {
        display.showContent();
    }

    @Override
    public Place getPlace() {
        return null;
    }

    @Override
    protected void onBind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
        display.clean();
    }

    public void refreshDisplay() {
        display.setTitle(data.getTitle());
        contentPresenter.refreshDisplay();
    }

    public void revealDisplay() {
    }
}
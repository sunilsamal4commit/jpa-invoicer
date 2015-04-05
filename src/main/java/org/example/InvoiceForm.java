package org.example;

import com.vaadin.cdi.ViewScoped;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Window;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.example.backend.Contact;
import org.example.backend.Invoice;
import org.example.backend.InvoiceRow;
import org.example.backend.Product;
import org.example.backend.service.ProductFacade;
import org.vaadin.viritin.fields.ElementCollectionField;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.fields.TypedSelect;
import org.vaadin.viritin.form.AbstractForm;
import org.vaadin.viritin.label.Header;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

@ViewScoped
public class InvoiceForm extends AbstractForm<Invoice> {
    
    @Inject
    ContactSelector to;
    
    @Inject
    ProductFacade productFacade;
    
    DateField invoiceDate = new DateField("invoiceDate");
    
    DateField dueDate = new DateField("dueDate");
    
    Header total = new Header("").setHeaderLevel(3);
    
    public static final class RowEditorModel {
        
        TypedSelect<Product> product = new TypedSelect<>(Product.class)
                .withSelectType(ComboBox.class)
                .withWidth("150px")
                .setInputPrompt("Pick a product");
        MTextField description = new MTextField().withFullWidth();
        MTextField quantity = new MTextField().withWidth("3em");
        MTextField unit = new MTextField().withWidth("3em");
        MTextField price = new MTextField().withWidth("3em");
        
    }
    
    ElementCollectionField<InvoiceRow> invoiceRows = new ElementCollectionField<>(
            InvoiceRow.class, RowEditorModel.class).expand("description")
            .withEditorInstantiator(() -> {
                RowEditorModel r = new RowEditorModel();
                r.product.setOptions(
                        productFacade.findActiveByInvoicer(getEntity().getInvoicer()));
                r.product.addMValueChangeListener(event -> {
                    if(event.getValue() != null) {
                        // Copy "default values" from Product to row
                        r.price.setValue(event.getValue().getPrice().toString());
                        r.unit.setValue(event.getValue().getUnit());

                        r.description.focus();
                    }
                });
                return r;
    });
    
    @PostConstruct
    void init() {
        // Allowing null in DB, but not when modified by the UI
        to.setRequired(true);
        to.addValidator(new AbstractValidator("Receiver must be set") {
            
            @Override
            protected boolean isValidValue(Object value) {
                return value != null;
            }
            
            @Override
            public Class getType() {
                return Contact.class;
            }
        });
        invoiceRows.addValueChangeListener(e -> updateTotal());
    }
    
    @Override
    protected Component createContent() {
        
        return new MVerticalLayout(
                new MHorizontalLayout(getToolbar()).add(total,
                        Alignment.MIDDLE_RIGHT)
                .withFullWidth(),
                new MVerticalLayout(
                        to,
                        new MHorizontalLayout(invoiceDate, dueDate),
                        invoiceRows
                )
        );
    }
    
    void updateTotal() {
        try {
            total.setText(getEntity().getTotal() + " €");
        } catch (Exception e) {
            total.setText("--");
        }
    }
    
    @Override
    public Window openInModalPopup() {
        final Window p = super.openInModalPopup();
        p.setWidth("80%");
        p.setHeight("95%");
        return p;
    }
    
}

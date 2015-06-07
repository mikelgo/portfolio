package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.stringWidth;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractSecurityTransactionModel.Properties;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;

import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SecurityTransactionDialog extends AbstractTransactionDialog
{
    private Client client;

    @Inject
    public SecurityTransactionDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client,
                    PortfolioTransaction.Type type)
    {
        super(parentShell, isBuySell(type) ? new BuySellModel(client, type) : new SecurityDeliveryModel(client, type));

        this.client = client;

        switch (type)
        {
            case BUY:
            case SELL:
                this.model = new BuySellModel(client, type);
                break;
            case DELIVERY_INBOUND:
            case DELIVERY_OUTBOUND:
                this.model = new SecurityDeliveryModel(client, type);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static boolean isBuySell(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
    }

    private AbstractSecurityTransactionModel model()
    {
        return (AbstractSecurityTransactionModel) this.model;
    }

    @PostConstruct
    public void preselectPorfolioAndSecurity()
    {
        // set portfolio only if exactly one exists
        // (otherwise force user to choose)
        List<Portfolio> activePortfolios = client.getActivePortfolios();
        if (activePortfolios.size() == 1)
            model().setPortfolio(activePortfolios.get(0));

        List<Security> activeSecurities = client.getActiveSecurities();
        if (!activeSecurities.isEmpty())
            model().setSecurity(activeSecurities.get(0));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // security

        ComboInput securities = new ComboInput(editArea, Messages.ColumnSecurity);
        securities.value.setInput(client.getActiveSecurities());
        securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
        securities.bindCurrency(Properties.securityCurrencyCode.name());

        // portfolio

        ComboInput portfolio = new ComboInput(editArea, Messages.ColumnPortfolio);
        portfolio.value.setInput(client.getActivePortfolios());
        portfolio.bindValue(Properties.portfolio.name(), Messages.MsgMissingPortfolio);
        portfolio.bindCurrency(Properties.accountName.name());

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTime valueDate = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);

        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate),
                        BeansObservables.observeValue(model, Properties.date.name()));

        // other input fields

        Input shares = new Input(editArea, Messages.ColumnShares);
        shares.bindValue(Properties.shares.name(), Messages.ColumnShares, Values.Share, true);

        Input quote = new Input(editArea, "x " + Messages.ColumnQuote); //$NON-NLS-1$
        quote.bindValue(Properties.quote.name(), Messages.ColumnQuote, Values.Quote, true);
        quote.bindCurrency(Properties.securityCurrencyCode.name());

        Input lumpSum = new Input(editArea, "="); //$NON-NLS-1$
        lumpSum.bindValue(Properties.lumpSum.name(), Messages.ColumnSubTotal, Values.Amount, true);
        lumpSum.bindCurrency(Properties.securityCurrencyCode.name());

        final Input exchangeRate = new Input(editArea, "x " + Messages.ColumnExchangeRate); //$NON-NLS-1$
        exchangeRate.bindExchangeRate(Properties.exchangeRate.name(), Messages.ColumnExchangeRate);
        exchangeRate.bindCurrency(Properties.exchangeRateCurrencies.name());

        final Input convertedLumpSum = new Input(editArea, "="); //$NON-NLS-1$
        convertedLumpSum.bindValue(Properties.convertedLumpSum.name(), Messages.ColumnSubTotal, Values.Amount, true);
        convertedLumpSum.bindCurrency(Properties.accountCurrencyCode.name());

        Input fees = new Input(editArea, sign() + Messages.ColumnFees);
        fees.bindValue(Properties.fees.name(), Messages.ColumnFees, Values.Amount, false);
        fees.bindCurrency(Properties.accountCurrencyCode.name());

        Input taxes = new Input(editArea, sign() + Messages.ColumnTaxes);
        taxes.bindValue(Properties.taxes.name(), Messages.ColumnTaxes, Values.Amount, false);
        taxes.bindCurrency(Properties.accountCurrencyCode.name());

        String label = getTotalLabel();
        Input total = new Input(editArea, "= " + label); //$NON-NLS-1$
        total.bindValue(Properties.total.name(), label, Values.Amount, true);
        total.bindCurrency(Properties.accountCurrencyCode.name());

        // note
        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER);
        context.bindValue(SWTObservables.observeText(valueNote, SWT.Modify),
                        BeansObservables.observeValue(model, Properties.note.name()));

        //
        // form layout
        //

        int width = stringWidth(total.value, "12345678,00"); //$NON-NLS-1$

        startingWith(securities.value.getControl(), securities.label).suffix(securities.currency)
                        .thenBelow(portfolio.value.getControl()).label(portfolio.label).suffix(portfolio.currency)
                        .thenBelow(valueDate)
                        .label(lblDate)
                        // shares - quote - lump sum
                        .thenBelow(shares.value).width(width).label(shares.label).thenRight(quote.label)
                        .thenRight(quote.value).width(width).thenRight(quote.currency).width(width)
                        .thenRight(lumpSum.label).thenRight(lumpSum.value).width(width).thenRight(lumpSum.currency);

        startingWith(quote.value).thenBelow(exchangeRate.value).width(width).label(exchangeRate.label)
                        .thenRight(exchangeRate.currency).width(width);

        startingWith(lumpSum.value)
                        // converted lump sum
                        .thenBelow(convertedLumpSum.value).width(width).label(convertedLumpSum.label)
                        .suffix(convertedLumpSum.currency)
                        // fees
                        .thenBelow(fees.value).width(width).label(fees.label).suffix(fees.currency)
                        // taxes
                        .thenBelow(taxes.value).width(width).label(taxes.label).suffix(taxes.currency)
                        // total
                        .thenBelow(total.value).width(width).label(total.label).suffix(total.currency)
                        // note
                        .thenBelow(valueNote).left(securities.value.getControl()).right(total.value).label(lblNote);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                String securityCurrency = model().getSecurityCurrencyCode();
                String accountCurrency = model().getAccountCurrencyCode();

                // make exchange rate visible if both are set but different

                boolean visible = securityCurrency.length() > 0 && accountCurrency.length() > 0
                                && !securityCurrency.equals(accountCurrency);

                exchangeRate.setVisible(visible);
                convertedLumpSum.setVisible(visible);
            }
        });

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model().getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    private String sign()
    {
        switch (model().getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                return "+ "; //$NON-NLS-1$
            case SELL:
            case DELIVERY_OUTBOUND:
                return "- "; //$NON-NLS-1$
            default:
                throw new UnsupportedOperationException();
        }
    }

    private String getTotalLabel()
    {
        switch (model().getType())
        {
            case BUY:
                return Messages.ColumnDebitNote;
            case DELIVERY_INBOUND:
                return "Wert der Einlieferung";
            case SELL:
                return Messages.ColumnCreditNote;
            case DELIVERY_OUTBOUND:
                return "Wert der Auslieferung";
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        model().setPortfolio(portfolio);
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSecurity(security);
    }

    public void setBuySellEntry(BuySellEntry entry)
    {
        if (!model().accepts(entry.getPortfolioTransaction().getType()))
            throw new IllegalArgumentException();
        model().setSource(entry);
    }

    public void setDeliveryTransaction(TransactionPair<PortfolioTransaction> pair)
    {
        if (!model().accepts(pair.getTransaction().getType()))
            throw new IllegalArgumentException();
        model().setSource(pair);
    }
}

package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.common.util.CurrencyFormatter;
import br.com.toppower.erp_toppower.organization.dto.OrganizationResponse;

import java.util.List;
import java.util.stream.Stream;

/**
 * View do emissor (Organization) já formatada para os templates de PDF.
 *
 * <p>Recebe um {@link OrganizationResponse} cru do backend e devolve um
 * objeto com strings pré-formatadas (CNPJ com máscara, endereço em
 * linhas separadas, etc.) para que os templates Thymeleaf não precisem
 * aplicar lógica — só renderizar.</p>
 *
 * <p>Construído por {@link PdfModelBuilder#buildIssuer}.</p>
 */
public record IssuerView(
        String corporateName,
        String tradeName,
        String cnpjFormatted,
        String stateRegistration,
        String municipalRegistration,
        String phone,
        String email,
        String logoUrl,
        List<String> addressLines
) {

    public static IssuerView from(OrganizationResponse org) {
        if (org == null) {
            return null;
        }
        return new IssuerView(
                nullToDash(org.corporateName()),
                nullToDash(org.tradeName()),
                formatCnpj(org.cnpj()),
                nullToDash(org.stateRegistration()),
                nullToDash(org.municipalRegistration()),
                nullToDash(org.phone()),
                nullToDash(org.email()),
                org.logoUrl(),
                buildAddressLines(org)
        );
    }

    /** Linha 1 do endereço: logradouro + número. */
    public String addressLine1() {
        return addressLines.isEmpty() ? "—" : addressLines.get(0);
    }

    /** Linha 2 do endereço: complemento + bairro. */
    public String addressLine2() {
        return addressLines.size() < 2 ? null : addressLines.get(1);
    }

    /** Linha 3 do endereço: cidade/UF + CEP. */
    public String addressLine3() {
        return addressLines.size() < 3 ? null : addressLines.get(2);
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String formatCnpj(String cnpj) {
        if (cnpj == null) return "—";
        String digits = cnpj.replaceAll("\\D", "");
        if (digits.length() != 14) return cnpj;
        return digits.substring(0, 2) + "."
                + digits.substring(2, 5) + "."
                + digits.substring(5, 8) + "/"
                + digits.substring(8, 12) + "-"
                + digits.substring(12);
    }

    private static List<String> buildAddressLines(OrganizationResponse org) {
        String line1 = Stream.of(org.street(), org.number())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
        String line2 = Stream.of(org.complement(), org.district())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + " — " + b)
                .orElse(null);
        String cityState = Stream.of(org.city(), org.state())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + "/" + b)
                .orElse(null);
        String line3 = (cityState == null) ? null
                : (org.zipCode() == null || org.zipCode().isBlank())
                        ? cityState
                        : cityState + " — CEP " + org.zipCode();
        return Stream.of(line1, line2, line3)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    /** Helper para templates: rótulo "Telefone: X" só se houver telefone. */
    public String phoneLabel() {
        return phone.equals("—") ? null : "Tel: " + phone;
    }

    /** Helper para templates: rótulo "E-mail: X" só se houver e-mail. */
    public String emailLabel() {
        return email.equals("—") ? null : email;
    }

    /** Helper: "Documento gerado em DD/MM/YYYY HH:mm" — gerado pelo template. */
    public static String generatedAt(java.time.Instant when) {
        if (when == null) return "—";
        return CurrencyFormatter.PT_BR == null ? when.toString() : when.toString();
    }
}
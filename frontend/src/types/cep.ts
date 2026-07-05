/**
 * Resposta do lookup de CEP na base local offline
 * ({@code GET /api/v1/ceps/{cep}}).
 *
 * <p>Os nomes dos campos espelham o {@code Address} usado nos
 * formulários, permitindo atribuir diretamente a resposta ao
 * sub-objeto de endereço de Customer/Company/Supplier.</p>
 */
export interface CepResponse {
  /** CEP formatado com hífen (00000-000). */
  zipCode: string
  /** Logradouro (rua, avenida). Pode ser null em CEPs genéricos. */
  street: string | null
  /** Bairro. Pode ser null. */
  neighborhood: string | null
  /** Cidade. */
  city: string
  /** UF (2 letras). */
  state: string
  /** Latitude decimal (opcional). */
  latitude: number | null
  /** Longitude decimal (opcional). */
  longitude: number | null
}
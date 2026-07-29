<?xml version="1.0" encoding="UTF-8"?>
<!-- ========================================================================= -->
<!--  PURPOSE:
      This XSLT transforms an incoming <Organization> feed into a minimal 
      structure used for organization validation or lookup.

      The XSLT performs the following:
        1. Retains only the OrganizationCode at the root level.
        2. Extracts only the required attributes from <Extn>:
              - ExtnCarrier
              - ExtnSAPOrganizationName
        3. Copies <BillingPersonInfo> exactly as-is.
        4. Suppresses all other elements and attributes such as:
              CorporatePersonInfo, ContactPersonInfo, Node, OrgRoleList, etc.

      This ensures only the essential data is passed to downstream services 
      (e.g., getOrganizationList API).
   ========================================================================= -->

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- ===================================================================== -->
  <!-- Template: Match root Organization element                             -->
  <!-- ===================================================================== -->
  <xsl:template match="/Organization">

    <Organization>
      <!-- Copy only OrganizationCode attribute -->
      <xsl:attribute name="OrganizationCode">
        <xsl:value-of select="@OrganizationCode"/>
      </xsl:attribute>

      <!-- ================================================================ -->
      <!-- Build <Extn> with only required attributes                      -->
      <!-- ================================================================ -->
      <xsl:if test="Extn">
        <Extn>
          <!-- Copy ExtnCarrier if present -->
          <xsl:if test="string(Extn/@ExtnCarrier) != ''">
            <xsl:attribute name="ExtnCarrier">
              <xsl:value-of select="Extn/@ExtnCarrier"/>
            </xsl:attribute>
          </xsl:if>

          <!-- Copy ExtnSAPOrganizationName if present -->
          <xsl:if test="string(Extn/@ExtnSAPOrganizationName) != ''">
            <xsl:attribute name="ExtnSAPOrganizationName">
              <xsl:value-of select="Extn/@ExtnSAPOrganizationName"/>
            </xsl:attribute>
          </xsl:if>
        </Extn>
      </xsl:if>

      <!-- Copy BillingPersonInfo exactly as-is -->
      <xsl:copy-of select="BillingPersonInfo"/>

    </Organization>
  </xsl:template>

  <!-- Default template: suppress all other nodes/attributes -->
  <xsl:template match="@*|node()"/>

</xsl:stylesheet>